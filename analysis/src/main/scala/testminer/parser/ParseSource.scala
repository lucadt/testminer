package testminer.parser

import java.io.File
import java.nio.file.{Files, Paths}
import java.security.MessageDigest
import java.util.concurrent.{Callable, Executors, TimeUnit}

import com.github.tototoshi.csv._
import org.apache.maven.repository.internal.MavenRepositorySystemUtils
import org.eclipse.aether.artifact.{Artifact, DefaultArtifact}
import org.eclipse.aether.collection.CollectRequest
import org.eclipse.aether.connector.basic.BasicRepositoryConnectorFactory
import org.eclipse.aether.graph.Dependency
import org.eclipse.aether.impl.DefaultServiceLocator
import org.eclipse.aether.repository.{LocalRepository, RemoteRepository}
import org.eclipse.aether.resolution.{ArtifactRequest, DependencyRequest}
import org.eclipse.aether.spi.connector.RepositoryConnectorFactory
import org.eclipse.aether.spi.connector.transport.TransporterFactory
import org.eclipse.aether.transport.file.FileTransporterFactory
import org.eclipse.aether.transport.http.HttpTransporterFactory
import org.eclipse.aether.util.artifact.JavaScopes
import org.eclipse.aether.util.filter.DependencyFilterUtils
import org.eclipse.aether.{DefaultRepositorySystemSession, RepositorySystem}

import scala.collection.JavaConversions._
import scala.collection.mutable.{ArrayBuffer, Queue}

object ParseSource {

  def main(args: Array[String]): Unit = {

    if (args.length == 0) {
      throw new RuntimeException("[TestMiner] Please specify the directory with the list file and the output directory")
    }

    val indexPath = args(0)
    val outputPath = args(1)
    val classifierToParse = "test-sources"

    val list = s"$indexPath/list.csv"

    val locator = MavenRepositorySystemUtils.newServiceLocator()
    val system = newRepositorySystem(locator)
    val session = newSession(system, indexPath + "/maven")

    val central = new RemoteRepository.Builder(
      "central",
      "default",
      "http://repo1.maven.org/maven2/").build()

    val remoteRepos = List(central)

    val classifiers = List("sources", "test-sources", "tests", "javadoc")

    def getArtifact(artifact: DefaultArtifact): Artifact = {
      try {
        val artifactRequest = new ArtifactRequest
        artifactRequest.setArtifact(artifact)
        artifactRequest.setRepositories(remoteRepos)
        val artifactResult = system.resolveArtifact(session, artifactRequest)
        val resArtifact = artifactResult.getArtifact
        /* println("> " + artifact) */
        resArtifact
      } catch {
        case _: Throwable =>
          /* println("> ERR " + artifact) */
          null
      }
    }

    val executor = Executors.newCachedThreadPool()
    val tasks = new Queue[Callable[Int]]

    val top100Reader = CSVReader.open(new File("data/top100.csv"))
    val top100LibsTemp = ArrayBuffer.empty[String]

    top100Reader.foreach { line =>
      val groupId = line.head
      val artifactId = line(1)
      top100LibsTemp += s"${groupId.trim}:${artifactId.trim}"
    }

    val top100Libs = top100LibsTemp.sorted.toList

    val toParse = ArrayBuffer.empty[(String, String, String)]
    val reader = CSVReader.open(new File(list))
    reader.foreach { line =>
      val groupId = line.head
      val artifactId = line(1)
      val version = line(2)
      toParse += ((groupId, artifactId, version))
    }
    reader.close

    toParse.zipWithIndex.foreach {
      case (tuple, numberOfLine) =>

        val (groupId, artifactId, version) = tuple

        var artifact: Artifact = null
        var artifactToParse: Artifact = null

        try {
          artifact = getArtifact(new DefaultArtifact(s"$groupId:$artifactId:jar:$version"))
          artifactToParse = getArtifact(new DefaultArtifact(s"$groupId:$artifactId:jar:$classifierToParse:$version"))
        } catch {
          case _: Throwable =>
            artifact = null
            artifactToParse = null
        }

        if (artifactToParse != null && artifact != null) {

          val artifactRepr = s"$groupId:$artifactId:jar:$version"
          val artifactHash = MessageDigest.getInstance("MD5").digest(artifactRepr.getBytes).map("%02x".format(_)).mkString

          val artifactExport = s"$outputPath/$artifactHash.json"
          val artifactExportPath = Paths.get(artifactExport)

          if (!Files.exists(artifactExportPath)) {

            val depsJar0 = List(artifact.getFile.getAbsolutePath)
            val depsSource0 = classifiers.flatMap { classifier =>
              val artifact = getArtifact(new DefaultArtifact(s"$groupId:$artifactId:jar:$classifier:$version"))
              if (artifact != null) {
                artifact.getClassifier match {
                  case "sources" => Some(artifact.getFile.getAbsolutePath)
                  case _ => None
                }
              } else {
                None
              }
            }

            try {

              val collectRequest = new CollectRequest(new Dependency(artifact, JavaScopes.COMPILE), List(central))

              val filter = DependencyFilterUtils.classpathFilter(JavaScopes.COMPILE)
              val request = new DependencyRequest(collectRequest, filter)
              val result = system.resolveDependencies(session, request)

              val notToFilter = result.getArtifactResults.exists { ar =>
                val depGroupId = ar.getArtifact.getGroupId
                val depArtifactId = ar.getArtifact.getArtifactId
                top100Libs.contains(s"$depGroupId:$depArtifactId")
              }

              if (notToFilter) {

                val depJars1 = result.getArtifactResults.map { ar =>
                  ar.getArtifact.getFile.getAbsolutePath
                }

                val depSource1 = result.getArtifactResults.flatMap { ar =>

                  val depGroupId = ar.getArtifact.getGroupId
                  val depArtifactId = ar.getArtifact.getArtifactId
                  val depVersion = ar.getArtifact.getVersion

                  classifiers.flatMap { classifier =>
                    val artifact = getArtifact(new DefaultArtifact(s"$depGroupId:$depArtifactId:jar:$classifier:$depVersion"))
                    if (artifact != null) {
                      artifact.getClassifier match {
                        case "sources" => Some(artifact.getFile.getAbsolutePath)
                        case _ => None
                      }
                    } else {
                      None
                    }
                  }

                }

                val depsJar = (depsJar0 ++ depJars1).distinct
                val depsSource = (depsSource0 ++ depSource1).distinct

                println(s"<< $artifact -- $numberOfLine >>")
                depsJar.foreach(item => println(" " + item))
                depsSource.foreach(item => println(" " + item))

                val task = new Callable[Int]() {
                  override def call(): Int = {
                    println(s"<< Parsing $artifact >>")
                    Parser.parse(
                      artifactToParse.getFile.getAbsolutePath,
                      artifactExport,
                      depsJar.toList,
                      depsSource.toList)
                    println(s"<< Parsing $artifact DONE >>")
                    0
                  }
                }

                tasks += task
              } else {
                println(s"[[ $artifact -- $numberOfLine ]]")

                result.getArtifactResults.foreach { ar =>
                  val depGroupId = ar.getArtifact.getGroupId
                  val depArtifactId = ar.getArtifact.getArtifactId
                  println(s" [[ $depGroupId:$depArtifactId ${top100Libs.contains(s"$depGroupId:$depArtifactId")} ]]")
                }
              }

              if (tasks.size >= 8) {
                try {
                  executor.invokeAll(tasks.toList, 30, TimeUnit.MINUTES)
                } catch {
                  case ex: Throwable =>
                    ex.printStackTrace()
                }
                tasks.clear
              }

            } catch {
              case _: Throwable =>
                println("> DE " + artifact)
            }

          }

        }

    }

    executor.invokeAll(tasks.toList)
    executor.shutdown()

  }

  def newRepositorySystem(locator: DefaultServiceLocator): RepositorySystem = {
    locator.addService(classOf[RepositoryConnectorFactory], classOf[BasicRepositoryConnectorFactory])
    locator.addService(classOf[TransporterFactory], classOf[FileTransporterFactory])
    locator.addService(classOf[TransporterFactory], classOf[HttpTransporterFactory])
    locator.getService(classOf[RepositorySystem])
  }

  def newSession(system: RepositorySystem, storagePath: String): DefaultRepositorySystemSession = {
    val session = MavenRepositorySystemUtils.newSession
    val localRepo = new LocalRepository(storagePath)
    session.setLocalRepositoryManager(system.newLocalRepositoryManager(session, localRepo))
    session
  }


}