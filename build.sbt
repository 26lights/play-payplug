name := "play-payplug"

releaseSettings

organization := "26lights"

scalaVersion := "2.11.2"

resolvers += "Typesafe repository" at "http://repo.typesafe.com/typesafe/releases/"

scalacOptions += "-feature"

scalacOptions += "-deprecation"

scalacOptions += "-unchecked"

fork in Test := true

libraryDependencies ++= Seq (
  "com.typesafe.play"  %% "play"          % "2.3.3",
  "com.typesafe.play"  %% "play-ws"       % "2.3.3",
  "org.bouncycastle"   %  "bcprov-jdk16"  % "1.46",
  "commons-codec"      %  "commons-codec" % "1.6",
  // test scope
  "com.typesafe.play"  %% "play-test"     % "2.3.3"  % "test",
  "org.scalatest"      %% "scalatest"     % "2.2.0"  % "test"
)
