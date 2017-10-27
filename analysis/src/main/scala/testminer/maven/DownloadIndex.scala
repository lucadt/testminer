package testminer.maven

import org.apache.lucene.search.BooleanClause.Occur
import org.apache.lucene.search.BooleanQuery
import org.apache.maven.index.GroupedSearchRequest
import org.apache.maven.index.Indexer
import org.apache.maven.index.MAVEN
import org.apache.maven.index.context.IndexCreator
import org.apache.maven.index.expr.SourcedSearchExpression
import org.apache.maven.index.search.grouping.GAGrouping
import org.apache.maven.index.updater.IndexUpdateRequest
import org.apache.maven.index.updater.IndexUpdater
import org.apache.maven.index.updater.WagonHelper
import org.apache.maven.wagon.Wagon
import org.apache.maven.wagon.events.TransferEvent
import org.apache.maven.wagon.observers.AbstractTransferListener
import org.codehaus.plexus.DefaultContainerConfiguration
import org.codehaus.plexus.DefaultPlexusContainer
import org.codehaus.plexus.PlexusConstants

import scala.collection.JavaConversions._

import java.io.File
import java.io.IOException
import java.util.Calendar

import com.github.tototoshi.csv._

object DownloadIndex {

  val IndexCachePath = "central-cache"
  val IndexPath = "central-index"

  def getToday: String = {

    val now = Calendar.getInstance()
    val day = now.get(Calendar.DAY_OF_MONTH)
    val month = now.get(Calendar.MONTH)
    val year = now.get(Calendar.YEAR)

    s"$year-${month + 1}-$day"

  }

  def main(args: Array[String]): Unit = {

    if (args.length == 0) {
      throw new RuntimeException("[TestMiner] Please specify the directory where to store the Maven Central index")
    }

    val indexPath = args(0)

    val indexPrefix = s"$indexPath/$getToday"

    val centralLocalCache = new File(s"$indexPrefix/$IndexCachePath")
    val centralIndexDir = new File(s"$indexPrefix/$IndexPath")

    val config = new DefaultContainerConfiguration
    config.setClassPathScanning(PlexusConstants.SCANNING_INDEX)
    val plexusContainer = new DefaultPlexusContainer(config)

    val indexer = plexusContainer.lookup(classOf[Indexer])
    val indexers = List(
      plexusContainer.lookup(classOf[IndexCreator], "min"),
      plexusContainer.lookup(classOf[IndexCreator], "jarContent"),
      plexusContainer.lookup(classOf[IndexCreator], "maven-plugin")
    )

    val centralContext = indexer.createIndexingContext(
      "central-context",
      "central",
      centralLocalCache,
      centralIndexDir,
      "http://repo1.maven.org/maven2",
      null,
      true,
      true,
      indexers)

    val indexUpdater = plexusContainer.lookup(classOf[IndexUpdater])
    val httpWagon = plexusContainer.lookup(classOf[Wagon], "http")

    println("Updating Index...")
    println("This might take a while on first run, so please be patient!")

    val listener = new AbstractTransferListener() {

      override def transferStarted(transferEvent: TransferEvent): Unit = {
        print("Downloading " + transferEvent.getResource.getName)
      }

      override def transferProgress(transferEvent: TransferEvent, buffer: Array[Byte], length: Int): Unit = {}

      override def transferCompleted(transferEvent: TransferEvent): Unit = {
        println(" - Done")
      }

    }

    val resourceFetcher = new WagonHelper.WagonFetcher(httpWagon, listener, null, null)
    val centralContextCurrentTimestamp = centralContext.getTimestamp
    val updateRequest = new IndexUpdateRequest(centralContext, resourceFetcher)
    val updateResult = indexUpdater.fetchAndUpdateIndex(updateRequest)

    if (updateResult.isFullUpdate) {
      println("Index full update.")
    } else if (updateResult.getTimestamp.equals(centralContextCurrentTimestamp)) {
      println("Index up-to date")
    } else {
      println("Index incremental update <-> " + centralContextCurrentTimestamp + " - " + updateResult.getTimestamp)
    }

    println()

    val query = new BooleanQuery()
    query.add(indexer.constructQuery(MAVEN.PACKAGING, new SourcedSearchExpression("jar")), Occur.MUST)

    try {

      val groupResponse = indexer.searchGrouped(new GroupedSearchRequest(query, new GAGrouping(), centralContext))

      val writer = CSVWriter.open(new File(indexPrefix + "/" + "list.csv"))
      groupResponse.getResults.entrySet().foreach { entry =>
        val ai = entry.getValue.getArtifactInfos.iterator().next()
        writer.writeRow(List(ai.getGroupId, ai.getArtifactId, ai.getVersion))
      }
      writer.close()
    } catch {
      case _: IOException =>
        indexer.closeIndexingContext(centralContext, false)
    }

  }
}