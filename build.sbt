import sbt.Keys._

name := "fleet-client"

organization := "com.monsanto.arch"

startYear := Some(2015)

// scala versions and options

scalaVersion := "2.11.7"

// These options will be used for *all* versions.

scalacOptions ++= Seq(
  "-deprecation"
  ,"-unchecked"
  ,"-encoding", "UTF-8"
  ,"-Xlint"
  // "-optimise"   // this option will slow your build
)

scalacOptions ++= Seq(
  "-Yclosure-elim",
  "-Yinline"
)

// These language flags will be used only for 2.10.x.

scalacOptions <++= scalaVersion map { sv =>
  if (sv startsWith "2.11") List(
    "-Xverify"
    ,"-feature"
    ,"-language:postfixOps"
  )
  else Nil
}

javacOptions ++= Seq("-Xlint:unchecked", "-Xlint:deprecation")

// dependencies

val akka = "2.3.11"
val spray = "1.3.3"

libraryDependencies ++= Seq (
  // -- testing --
   "org.scalatest"              %% "scalatest"                % "2.2.4"  % "test"
  // -- Akka --
  ,"com.typesafe.akka"          %% "akka-actor"               % akka
  ,"com.typesafe.akka"          %% "akka-slf4j"               % akka
  // -- Spray --
  ,"io.spray"                   %% "spray-client"             % spray
  ,"io.spray"                   %% "spray-testkit"            % spray    % "test"
  // -- json --
  ,"io.spray"                   %% "spray-json"               % "1.3.2"
).map(_.force())

resolvers ++= Seq(
  "spray repo" at "http://repo.spray.io",
  Resolver.sonatypeRepo("releases"),
  Resolver.sonatypeRepo("snapshots")
)

// for ghpages

site.settings

site.includeScaladoc()

ghpages.settings

git.remoteRepo := "git@github.com:MonsantoCo/fleet-client.git"

// for bintray

bintrayOrganization := Some("monsanto")

licenses += ("BSD", url("http://opensource.org/licenses/BSD-3-Clause"))
