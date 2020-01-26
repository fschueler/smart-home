name := """smart-home"""

val commonSettings = Seq(
  organization := "de.fschueler",
  version := "1.0-SNAPSHOT",
  scalaVersion := "2.13.1"
)

lazy val root = (project in file(".")).
  settings(commonSettings).
  enablePlugins(PlayScala)

pipelineStages := Seq(digest)

libraryDependencies ++= Seq(
  ws,
  "com.softwaremill.macwire" %% "macros" % "2.3.3" % "provided",
  "ch.qos.logback"  %  "logback-classic"   % "1.2.3",
  "org.scalatestplus.play" %% "scalatestplus-play" % "5.0.0" % "test"
)