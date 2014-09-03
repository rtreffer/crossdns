import AssemblyKeys._

import NativePackagerKeys._

import com.typesafe.sbt.packager.archetypes.ServerLoader.{Upstart, SystemV}

organization := "de.measite"

name         := "crossdns"

version      := "1.0-SNAPSHOT"

scalaVersion := "2.11.2"

scalacOptions ++= Seq(
  "-deprecation",
  "-feature",
  "-unchecked"
)

resolvers += "Base" at "http://repo1.maven.org/maven2/"

resolvers += "SonatypeReleases" at "http://oss.sonatype.org/content/repositories/releases/"

resolvers += "JPcap" at "http://kindsoft.cn/maven2/"

resolvers += "JPcap_new" at "http://ubiquitos.googlecode.com/svn/trunk/src/Java/maven/"

libraryDependencies += "org.scala-lang" % "scala-compiler" % "2.11.2"

libraryDependencies += "org.scala-lang" % "scala-reflect" % "2.11.2"

libraryDependencies += "org.scala-lang" % "scala-library" % "2.11.2"

libraryDependencies += "jpcap" % "jpcap" % "1.0"

libraryDependencies += "jcifs" % "jcifs" % "1.3.17"

libraryDependencies += "de.measite.minidns" % "minidns" % "0.1.3"

libraryDependencies += "net.java.dev.jna" % "jna" % "4.1.0"

libraryDependencies += "net.databinder" %% "unfiltered-netty-server" % "0.8.1"

libraryDependencies += "net.databinder" %% "dispatch-core" % "0.8.10"

libraryDependencies += "net.databinder" %% "unfiltered-json4s" % "0.8.1"

fork := true

assemblySettings

mergeStrategy in assembly <<= (mergeStrategy in assembly) { (old) =>
  {
    case PathList("META-INF", "io.netty.versions.properties") => MergeStrategy.concat
    case x => old(x)
  }
}

packageArchetype.java_server


packageDescription in Debian := "crossing the boundary between autodiscovery and dns"

maintainer in Debian := "treffer@measite.de <Rene Treffer>"

packageBin in Debian <<= debianJDebPackaging in Debian

debianPackageDependencies in Debian ++= Seq("java2-runtime", "bash (>= 2.05a-11)")

linuxPackageMappings in Debian <+= (baseDirectory) map { (basedir) => (packageMapping(
  (basedir / "crossdns.conf.example") -> ("/etc/crossdns/crossdns.conf.example"))
  withUser "root" withGroup "root" withPerms "0644" )
}

serverLoading in Debian := SystemV

bashScriptExtraDefines += """addJava "-Xmx140m""""

