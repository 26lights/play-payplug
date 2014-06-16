package twentysix.payplug.models

import play.api.libs.json.JsValue
import org.joda.time.DateTime
import org.joda.time.DateTime._

object PayplugPaymentStatus extends Enumeration {
  type PayplugPaymentStatus = Value
  val Pending, Paid, Reimbursed, Unknown = Value
}

import twentysix.payplug.models.PayplugPaymentStatus.PayplugPaymentStatus

case class PayplugPayment(userId: Long,
                          details: JsValue,
                          amount: Long,
                          status: PayplugPaymentStatus,
                          userFirstName: Option[String] = None,
                          userLastName: Option[String] = None,
                          userEmail: Option[String] = None,
                          creationDate:DateTime = now(),
                          id: Option[Long] = None)
