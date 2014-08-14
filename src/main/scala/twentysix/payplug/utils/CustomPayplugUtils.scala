package twentysix.payplug.utils

import java.net.URLEncoder
import java.security.{PublicKey, Signature, KeyPair}
import org.apache.commons.codec.binary.Base64
import java.io.FileReader
import play.api.libs.json.{JsObject, Json}
import org.joda.time.DateTime._
import play.api.mvc.{RawBuffer, Request}
import twentysix.payplug.models.{PayplugPaymentStatus, PayplugPayment}
import org.bouncycastle.openssl.PEMReader


case class CustomPayplugUtils(applicationName: String, returnUrl: String, ipnUrl: String, baseUrl: String, pkFileName: String, pubKeyFileName: String) {

  lazy val keyPair = new PEMReader(new FileReader(pkFileName)).readObject().asInstanceOf[KeyPair]
  lazy val publicKey = new PEMReader(new FileReader(pubKeyFileName)).readObject().asInstanceOf[PublicKey]


  /**
   * Update a PaymentView using data returned by the IPN (Instant Payment Notification) of PayPlug
   *
   * Body content (from https://www.payplug.fr/static/default/docs/developer/v0.7/Integration_PayPlug.pdf ):
   *
   *  id_transaction      Identifiant de transaction PayPlug. Nous vous conseillons de l’enregistrer et l’associer dans votre site à la commande concernée.
   *  state               Le statut de la transaction (paid, refunded)
   *  customer            Les données du champ ‘customer’ fourni lors du paiement.
   *  order               Les données du champ ‘order’ fourni lors du paiement.
   *  custom_data         Les données du champ ‘custom_data’ fourni lors du paiement.
   *  origin              Information sur votre site web (ex : ‘My Website 1.2 payplug-php0.9 PHP 5.3’) plus des données calculées par la librairie elle même.
   *
   * @param payment
   * @param request
   * @return
   */
  def updatePaymentFromIpn(payment: PayplugPayment, request: Request[RawBuffer]): PayplugPayment = {
    val headers = request.headers
    val rawBody = request.body.asBytes().get
    val body = Json.parse(rawBody)

    headers.get("PayPlug-Signature").map {b64Signature =>
      val signature = Base64.decodeBase64(b64Signature)
      val signer = Signature.getInstance("SHA1withRSA", "BC")
      signer.initVerify(publicKey)
      signer.update(rawBody)
      if(!signer.verify(signature)) {
        throw new IllegalArgumentException(s"Incorrect IPN signature")
      }
    }.orElse{
      throw new IllegalArgumentException(s"IPN signature not found")
    }

    val transactionId = body \ "id_transaction"
    val state = (body \ "state").as[String]
    val userId = (body \ "customer").as[String]
    val paymentId = (body \ "order").as[String]
    val details = Json.parse((body \ "custom_data").as[String]).asInstanceOf[JsObject]
    val origin = body \ "origin"

    if(payment.userId != userId) {
      throw new IllegalArgumentException(s"Parameter 'customer' ($userId) does not correspond to the payment userId (${payment.userId}).")
    }
    if(payment.id.get != paymentId) {
      throw new IllegalArgumentException(s"Parameter 'order' ($paymentId) does not correspond to the payment id (${payment.id.get}).")
    }
    details.fields.foreach{ case (k, v) =>
      val vPayment = payment.details \ k
      if(vPayment != v)
        throw new IllegalArgumentException(s"Parameter 'custom_data[$k]' (${v.toString()}}) does not correspond to the payment details[$k] (${vPayment.toString()}).")
    }

    var updatedDetails = payment.details.as[JsObject] ++ Json.obj("transaction_id" -> transactionId, "origin" -> origin)
    val updatedStatus = statusFromStateString(state)
    if(updatedStatus == PayplugPaymentStatus.Paid) {
      updatedDetails ++= Json.obj("payment_date" -> now())
    } else if(updatedStatus == PayplugPaymentStatus.Reimbursed) {
      updatedDetails ++= Json.obj("reimbursement_date" -> now())
    }
    payment.copy(details = updatedDetails, status = updatedStatus)
  }

  def statusFromStateString(state: String) = {
    state match {
      case "paid" => PayplugPaymentStatus.Paid
      case "refunded" => PayplugPaymentStatus.Reimbursed
      case _ => PayplugPaymentStatus.Unknown
    }
  }

  def paramsMapToQueryString(data: Map[String, String]): String = {
    data.map{ case (k, v) =>
      URLEncoder.encode(k, "UTF-8")+"="+URLEncoder.encode(v, "UTF-8")
    }.mkString("&")
  }

  def getSignature(message: String):String = {
    val signer = Signature.getInstance("SHA1withRSA", "BC")
    signer.initSign(keyPair.getPrivate)
    signer.update(message.getBytes("UTF-8"))
    val signatureBytes = signer.sign
    Base64.encodeBase64String(signatureBytes)
  }

  def getUrl(params: Map[String, String]) = {
    val url_params: String = paramsMapToQueryString(params)
    val signature: String = getSignature(url_params)
    val dataSign = Map(
      "data" -> Base64.encodeBase64String(url_params.getBytes),
      "sign" -> signature
    )
    baseUrl + "?" + paramsMapToQueryString(dataSign)
  }

  def paymentUrl(payment: PayplugPayment) = {
    val paymentIdStr = payment.id.get
    val params = Map(
      "amount" -> payment.amount.toString,
      "currency" -> "EUR",
      "ipn_url" -> ipnUrl.replaceAllLiterally(":id", paymentIdStr),
      "return_url" -> returnUrl,
      "order" -> paymentIdStr,
      "customer" -> payment.userId,
      "custom_data" -> payment.details.toString,
      "origin" -> applicationName
    ) ++ payment.userEmail.map{"email" -> _} ++ payment.userFirstName.map{"first_name" -> _} ++ payment.userLastName.map{"last_name" -> _}

    getUrl(params)
  }

}
