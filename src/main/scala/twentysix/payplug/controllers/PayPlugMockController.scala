package twentysix.payplug.controllers

import play.api.mvc.{Controller, Action}
import org.apache.commons.codec.binary.Base64
import java.security.Signature
import java.net.URLDecoder
import play.api.Play.current
import play.api.Logger
import play.api.libs.ws.WS
import play.api.libs.json.Json
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import twentysix.payplug.utils.PayplugUtils

object PayPlugMockController extends Controller {

  val payplugUtils = PayplugUtils()

  def pay(data: String, sign: String) = Action.async {

    // Decode parameters
    var dataContent = new String(Base64.decodeBase64(data), "UTF-8")
    val signature = Base64.decodeBase64(sign)

    // Check incoming data signature
    val publicKey = payplugUtils.keyPair.getPublic
    val signer = Signature.getInstance("SHA1withRSA", "BC")
    signer.initVerify(publicKey)
    signer.update(dataContent.getBytes)
    if(!signer.verify(signature)) {
      Future(BadRequest("Invalid signature"))
    } else {
      // Get incoming data values
      dataContent = URLDecoder.decode(dataContent, "UTF-8")
      val receivedParams:Map[String, String] = dataContent.split("&").toList.map(_.trim).filter(_.length > 0).map{param :String =>
        val pair = param.split("=")
        pair(0) -> pair(1)
      }.toMap
      Logger.info("Mock pay for: ")
      receivedParams.foreach{case (k, v) => Logger.info(s"$k = $v")}

      // Create IPN parameters
      val ipnParams = Json.obj(
        "id_transaction" -> "MockTransaction",
        "state" -> "paid",
        "customer" -> receivedParams("customer"),
        "order" -> receivedParams("order"),
        "custom_data" -> receivedParams("custom_data"),
        "origin" -> (receivedParams("origin") + " - MockApp")
      )

      // Sign it with the private key
      val ipnSignature = payplugUtils.getSignature(ipnParams.toString())

      // Call the IPN
      val ipnUrl = receivedParams("ipn_url")
      WS.url(ipnUrl).withHeaders("PayPlug-Signature" -> ipnSignature).post(ipnParams).map{ipnResponse =>
        if(ipnResponse.status < 200 || ipnResponse.status > 299) {
          val response = Status(ipnResponse.status)(ipnResponse.body)
          ipnResponse.header("Content-type").map{ct => response.withHeaders("Content-type" -> ct)}.getOrElse(response)
        } else {
          // Redirect to the return URL
          val returnUrl = receivedParams("return_url")
          Redirect(returnUrl,302)
        }
      }
    }
  }

}
