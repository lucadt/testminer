package testminer.parser

import java.io._
import java.net.URI
import java.nio.file._
import java.nio.file.attribute.BasicFileAttributes

import spray.json._
import testminer.ExportJsonProtocol._
import testminer._

import scala.collection.JavaConversions._
import scala.collection.mutable.ArrayBuffer

object Parser {

  def exportTypes(filePath: Path, types: List[Type]): Unit = {

    var bw: BufferedWriter = null
    var outputStream: FileOutputStream = null
    try {
      outputStream = new FileOutputStream(filePath.normalize.toString)
      bw = new BufferedWriter(new OutputStreamWriter(outputStream))
      val jsonCall = types.toList.toJson.toString
      bw.write(jsonCall, 0, jsonCall.length)
    } catch {
      case ex: Throwable =>
        ex.printStackTrace()
        Files.deleteIfExists(filePath)

    } finally {
      try {
        if (bw != null) {
          bw.close()
          outputStream.close()
        }
      } catch {
        case ex: Throwable =>
          ex.printStackTrace()
          System.exit(-1)
      }
    }

  }

  val jarOrZipProperties = Map("create" -> "false")

  val jarMatcher: PathMatcher = FileSystems.getDefault.getPathMatcher("glob:*.jar")
  val javaSourceMatcher: PathMatcher = FileSystems.getDefault.getPathMatcher("glob:*.java")

  def parse(file: String, outputFile: String, depsJars: List[String], depsSources: List[String]): Unit = {

    val depsSourcesInc: List[String] = depsSources ++ List(file)

    def parseJar(jarPath: Path, parse: ((Path, List[String], List[String]) => List[Type])) = {

      val types = ArrayBuffer.empty[Type]
      try {
        val jarUri = URI.create("jar:file:" + file)
        val jarFs = FileSystems.newFileSystem(jarUri, jarOrZipProperties)
        val rootJarPath = jarFs.getPath("/")
        Files.walkFileTree(rootJarPath, new SimpleFileVisitor[Path] {
          override def visitFile(file: Path, attributes: BasicFileAttributes): FileVisitResult = {
            if (javaSourceMatcher.matches(file.getFileName)) {
              try {
                types ++= parse(file, depsJars, depsSourcesInc)
              } catch {
                case ex: Throwable =>
                  ex.printStackTrace()
              }
            }
            FileVisitResult.CONTINUE
          }
        })
        jarFs.close()
      } catch {
        case ex: Throwable =>
          ex.printStackTrace()
      }
      types.toList
    }

    val jarFilePath = Paths.get(file)
    val types = parseJar(jarFilePath, testminer.parser.EclipseParser.types)

    val mapTypes = types.flatMap { t =>
      t match {
        case Class(name, _, _) =>
          Some(name -> t)
        case Interface(name, _) =>
          Some(name -> t)
        case _ =>
          None
      }
    }.toMap

    def equals(s0: Signature, s1: Signature) =
      s0.name == s1.name &&
        s0.params == s1.params &&
        s0.typeArgs == s1.typeArgs

    def replaceTuple(tuple: Tuple) = tuple match {
      /*
         Replace the signature with the declared method to get
         for each tuple the parameters names
       */
      case Call(targetType, targetMethod, args) =>

        if (mapTypes.contains(targetType.name)) {

          val methods = mapTypes(targetType.name) match {
            case Class(_, _, methods) =>
              methods
            case Interface(_, methods) =>
              methods
          }

          val found = methods.find({
            case Method(signature, _) =>
              equals(signature, targetMethod)
            case _ =>
              false
          })

          val aTargetMethod = found match {
            case Some(method) => method match {
              case mt: Method =>
                mt.signature
              case _ =>
                targetMethod
            }
            case None =>
              targetMethod
          }

          Call(targetType, aTargetMethod, args)

        } else {
          tuple
        }

      case _ =>
        tuple

    }

    val finalTypes = types.flatMap {

        case Class(name, superName, functions) =>
          Some(Class(name, superName, functions.flatMap {

            case Method(signature, tuples) =>

              val filteredTuples = tuples.filter {
                case Call(_, _, args) =>
                  args.exists(_ != Unknown)
                case NewObject(_, args) =>
                  args.exists(_ != Unknown)
                case _ =>
                  true
              }

              Some(Method(signature, filteredTuples.map(replaceTuple)))

            case _ =>
              None

          }))

        case _ =>
          None

    }

    val chunkFile = Paths.get(outputFile)
    Files.createDirectories(chunkFile.getParent)
    Files.createFile(chunkFile)
    exportTypes(chunkFile, finalTypes)

  }

}