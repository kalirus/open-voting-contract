import Dependencies._

ThisBuild / scalaVersion := "2.12.8"
ThisBuild / version := "0.1.0-SNAPSHOT"
ThisBuild / organization := "com.wavesplatform"
ThisBuild / organizationName := "wavesplatform"

lazy val root = (project in file("."))
  .settings(
    name := "open-voting-contract",
    libraryDependencies ++= Seq(sttp, playJson, scalaTest % Test)
  )

mainClass in assembly := Some("com.wavesplatform.contract.Main")

assemblyMergeStrategy in assembly := {
  case PathList("module-info.class") => MergeStrategy.discard
  case x => (assemblyMergeStrategy in assembly).value(x)
}
