package twentysix.payplug.utils

import play.api.Play.current

object PayplugUtils {
  val applicationName = current.configuration.getString("application.title").getOrElse(throw new IllegalArgumentException("Incorrect config parameter for application.title"))
  val returnUrl = current.configuration.getString("payplug.returnUrl").getOrElse(throw new IllegalArgumentException("Incorrect config parameter for payplug.returnUrl"))
  val ipnUrl = current.configuration.getString("payplug.ipnUrl").getOrElse(throw new IllegalArgumentException("Incorrect config parameter for payplug.ipnUrl"))
  val baseUrl = current.configuration.getString("payplug.baseUrl").getOrElse(throw new IllegalArgumentException("Incorrect config parameter for payplug.baseUrl"))
  val pkFileName = current.configuration.getString("payplug.privateKeyFile").getOrElse(throw new IllegalArgumentException("Incorrect config parameter for payplug.privateKeyFile"))
  val pubKeyFileName = current.configuration.getString("payplug.publicKeyFile").getOrElse(throw new IllegalArgumentException("Incorrect config parameter for payplug.publicKeyFile"))

  def apply() = CustomPayplugUtils(applicationName, returnUrl, ipnUrl, baseUrl, pkFileName, pubKeyFileName)
}
