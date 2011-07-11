package net.renalias.translate

import net.renalias.logging.Logging

trait Bing extends BaseTranslationAPI with Logging {
  this: BingConfig =>
  val baseUrl = "http://api.microsofttranslator.com/V2/Http.svc/"
  protected val httpErrorCodes = List(400, 401, 402, 403)

  def translate(text:String, from:Language, to:Language) = {
    import dispatch._
    import scala.xml._

    val targetUrl = baseUrl + "Translate?" + Helpers.buildQuery(List("appId" -> this.appId, "text" -> text, "from" -> from.langCode, "to" -> to.langCode))
    log.debug("Bing target URL = " + targetUrl)

    try {
      // make the request, serialize to XML and process the resulting NodeSeq to extract what we need
      // using the when() clause we make sure that the request handler is only run when the http response code
      // is not 40x
      val newText = newExecutor().when({code => !httpErrorCodes.exists(_ == code)})(url(targetUrl) <> { result =>
        (result \\ "string").text
      })

      Right(TranslationResult(newText))
    } catch {
      case ex:StatusCode => Left(TranslationFailure("http-error", "The server returned an HTTP error", Some(ex)))
      case ex:Exception => Left(TranslationFailure("error", "Error making the HTTP request", Some(ex)))
    }
  }
}