=============
play-payplug
=============

*play-payplug* is a Play library for simplifying payment using PayPlug in your Play application.

.. |26lights| image:: 26lights.png
    :width: 64px
    :align: middle
    :target: http://www.26lights.com

Its development is supported by |26lights|.

===========
Usage
===========

Sbt dependency
===============

To use play-payplug in your sbt based project, you should add a resolver for the 26lights public repository:

.. code-block:: scala

  resolvers += "26Lights releases" at "http://build.26source.org/nexus/content/repositories/public-releases"

and add play-payplug to your library dependencies:

.. code-block:: scala

  libraryDependencies ++= Seq (
    "26lights"  %% "play-payplug"  % "0.1.0"
  )

Configuration
==============

Before configuring your application, you'll need to retrive your PayPlug parameters.

To do this, go to https://www.payplug.fr/portal/ecommerce/autoconfig and log-in with your PayPlug credentials.

Write down the ``url`` parameter, copy the content of the ``yourPrivateKey`` parameter to a ``.pem`` file and the content of the ``payplugPublicKey`` parameter to a ``.pub`` file.

If you want to use the default ``PayplugUtils`` implementation, you will need to add some informations into your ``application.conf`` (otherwise, you should use ``CustomPayplugUtils`` and pass to it all the needed parameters):

.. code-block:: nginx

  payplug {
    baseUrl = "http://localhost:9000/payplug"                 # Base PayPlug URL
    privateKeyFile = "conf/payplug.pem"                       # Path to a file containing your PayPlug private key
    publicKeyFile = "conf/payplug.pub"                        # Path to a file containing PayPlug public key
    returnUrl = "http://localhost:9000/"                      # URL on which the user will be redirected upon payment completion
    ipnUrl = "http://localhost:9000/api/payment/:id/notify"   # URL called by PayPlug to confirm payment (:id will be replaced by your payment id)
  }

The PayPlug base URL, your private key and PayPlug public key are the one you retrieved from the PayPlug API, the last two should point to meaningful URL in your application.

Sample source code
===================

In order to use it in your application you will need to:

instantiate the PayPlug utility class
  the simplest way to do it is to use the ``PayPlugUtils`` implementation which uses the ``application.conf`` settings as described above.
  To do this, simply declare it like this:

  .. code-block:: scala

    val payplugUtils = PayplugUtils()


generate a payment URL
  this is an URL to PayPlug payment website, this is where your user will enter its payment details and where they will be validated by PayPlug:

  .. code-block:: scala

    def paymentUrl(amount: Long, userId: Long, productName: String, userFirstName: Option[String] = None, userLastName: Option[String] = None, userEmail: Option[String] = None): String = {
      val paymentDetails = Json.obj("productName" -> productName) // Could be anything, it is meant to store any data related to the payment
      val payment = PayplugPayment(userId, paymentDetails, amount, PayplugPaymentStatus.Pending, userFirstName, userLastName, userEmail)
      val persistedPayment =  ??? // Here you will need to persist your payment object and give it an id
      payplugUtils.paymentUrl(persistedPayment)
    }

have an IPN Action
  this should be a publicy internet-accessible route which will handle the notification of the payment like this:
  
  .. code-block:: scala

    def notify(paymentId: Long) = Action(parse.raw) { request =>
      val payment = ??? // Here you will need to retrieve your payment from your persistance
      val updated = payplugUtils.updatePaymentFromIpn(payment, request)
      // Here you should persist the updated payment
      if(updated.status == PayplugPaymentStatus.Paid) {
        // The payment is now validated, you should do something about it (continue to shipping process, activate rights, and so on...)
      }
      NoContent
    }


Running in mocked (non-production) environnment
=================================================

To be able to run your application without connection to PayPlug, you will need to use the ``PayPlugMockController``.

To do this, simply add it to your routes:

.. code-block:: nginx

  GET           /payplug                   twentysix.payplug.controllers.PayPlugMockController.pay(data, sign)

and make sure your ``payplug.baseUrl`` configuration point to this route.

It will them check if your payment data is correct and then call the IPN url so that everything will run like it should in production (without the real payment part).
