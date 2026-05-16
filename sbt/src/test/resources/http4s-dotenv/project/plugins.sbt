updateOptions := updateOptions.value.withLatestSnapshots(false)

resolvers += Resolver.mavenLocal

addSbtPlugin("me.seroperson" % "sbt-live-reload" % sys.props("project.version"))
addSbtPlugin("com.eed3si9n" % "sbt-buildinfo" % "0.13.1")
addSbtPlugin("nl.gn0s1s" % "sbt-dotenv" % "3.2.0")
