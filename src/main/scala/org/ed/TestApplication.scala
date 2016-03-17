package org.ed

import io.dropwizard.Application
import io.dropwizard.setup.Environment
import org.ed.AnnotationProxies._
import org.ed.ByteBuddyTest.{EndPoint, ResouceBuilder}
import org.ed.conf.TestConf


class TestApplication extends Application[TestConf] {
  override def run(t: TestConf, environment: Environment): Unit = environment.jersey().register(generateAction)

  def generateAction: Any = {
    val endpoint =
      EndPoint
        .annotations(
          GET,
          Path("/foo/{blah}"),
          Produces(s"application/foo+json"))
        .firstParameter(QueryParam("blah"))
        .secondParameter(QueryParam("foo"))
        .thirdParameter(PathParam("blah"))
        .function((a: String, i: Int, blah: String) => s"""{"hello": "$a$i$blah"}""")

    ResouceBuilder.newResouce("TestClass", Path(s"/base"))
      .endpoint(endpoint)
      .build()
  }
}

object TestApplication {
  def main(args: Array[String]): Unit = new TestApplication().run(args: _*)
}
