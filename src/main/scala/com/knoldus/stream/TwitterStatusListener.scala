package com.knoldus.stream


import com.knoldus.kafka.producer.Producer
import org.jsoup.Jsoup
import org.slf4j.{Logger, LoggerFactory}
import twitter4j.{StatusListener, _}

class TwitterStatusListener extends StatusListener with JsonHelper {

  val logger: Logger = LoggerFactory.getLogger(this.getClass())

  val kafkaProducer = Producer("localhost:9092")

  val EMPTY_STRING = ""

  def onStatus(status: Status) {
    logger.info("Got tweet from user " + Option(status.getUser).fold("Unknown")(_.getName))
    val tweetJson = write(getAllField(status))
    kafkaProducer.send("tweet_queue", tweetJson)
  }

  def onDeletionNotice(statusDeletionNotice: StatusDeletionNotice) {
    logger.info("Got a status deletion notice id:" + statusDeletionNotice.getStatusId)
  }

  def onTrackLimitationNotice(numberOfLimitedStatuses: Int) {
    logger.info("Got track limitation notice:" + numberOfLimitedStatuses)
  }

  def onScrubGeo(userId: Long, upToStatusId: Long) {
    logger.info("Got scrub_geo event userId:" + userId + " upToStatusId:" + upToStatusId)
  }

  def onStallWarning(warning: StallWarning) {
    logger.info("Got stall warning:" + warning)
  }

  def onException(ex: Exception) {
    logger.error("Error in getting tweet", ex)
  }

  private def getAllField(status: Status): Map[String, String] = {
    val text = status.getText().replaceAll("\n", " ")
    Map(
      "id" -> status.getId().toString,
      "media_type" -> "twitter",
      "author_username" -> Option(status.getUser).fold(EMPTY_STRING)(_.getScreenName),
      "text" -> text,
      "clean_text" -> Jsoup.parse(text).text(),
      "created_at" -> status.getCreatedAt().toString,
      "retweets" -> status.getRetweetCount().toString
    )

  }
}

