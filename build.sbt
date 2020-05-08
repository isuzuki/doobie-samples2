name := "doobie-samples"

version := "0.1"

scalaVersion := "2.13.1"

val doobieVersion = "0.8.8"

libraryDependencies ++= Seq(
  "org.tpolecat" %% "doobie-core" % doobieVersion,
  "org.tpolecat" %% "doobie-hikari" % doobieVersion,
  "org.tpolecat" %% "doobie-postgres" % doobieVersion
)
