package org.ed

import java.lang.annotation.Annotation

import net.bytebuddy.ByteBuddy
import net.bytebuddy.dynamic.DynamicType.Builder
import net.bytebuddy.dynamic.DynamicType.Builder.MethodDefinition
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy
import net.bytebuddy.implementation.MethodDelegation
import net.bytebuddy.jar.asm.Opcodes._

import scala.reflect.ClassTag

object ByteBuddyTest extends App {


  class ResouceBuilder private(private val builder: Builder[Any], private val methodMap: Map[String, Any]) {
    private def addAnnotations(mb: MethodDefinition.ReceiverTypeDefinition[Any], ans: Seq[Annotation]) =
      ans.toList match {
        case Nil => mb
        case an :: rest => rest.foldLeft(mb.annotateMethod(an))((mbi, ani) => mbi.annotateMethod(ani))
      }

    private def createMethod(b: Builder[Any], name: String, returnType: Class[_]) = b.defineMethod(name, returnType, ACC_PUBLIC)

    private val nextMethodName = "a%d".format(methodMap.size)

    def endpoint[T](d: EndPoint[() => T])(implicit c: ClassTag[T]) = {
      new ResouceBuilder(implementDelegate(builder, nextMethodName, d, c.runtimeClass), methodMap + (nextMethodName -> d.d))
    }

    def endpoint[T, I](d: EndPoint[(I) => T])(implicit c: ClassTag[T], ci: ClassTag[I]) = {
      new ResouceBuilder(implementDelegate(builder, nextMethodName, d, c.runtimeClass, ci.runtimeClass), methodMap + (nextMethodName -> d.d))
    }

    def endpoint[T, I, J](d: EndPoint[(I, J) => T])(implicit c: ClassTag[T], ci: ClassTag[I], cj: ClassTag[J]) =
      new ResouceBuilder(implementDelegate(builder, nextMethodName, d, c.runtimeClass, ci.runtimeClass, cj.runtimeClass), methodMap + (nextMethodName -> d.d))

    def endpoint[T, I, J, K](d: EndPoint[(I, J, K) => T])(implicit c: ClassTag[T], ci: ClassTag[I], cj: ClassTag[J], ck: ClassTag[K]) =
      new ResouceBuilder(implementDelegate(builder, nextMethodName, d, c.runtimeClass, ci.runtimeClass, cj.runtimeClass, ck.runtimeClass), methodMap + (nextMethodName -> d.d))


    private def implementDelegate(b: Builder[Any],
                                  name: String,
                                  functionType: Class[_],
                                  ans: Seq[Annotation],
                                  returnType: Class[_],
                                  pars: Class[_]*) = {
      val methodBuilder: MethodDefinition.ReceiverTypeDefinition[Any] = createMethod(b, name, returnType)
        .withParameters(pars: _*)
        .intercept(MethodDelegation.toInstanceField(functionType, name))

      addAnnotations(methodBuilder, ans)
    }


    private def implementDelegate(b: Builder[Any],
                                  name: String,
                                  param: EndPoint[_],
                                  returnType: Class[_],
                                  pars: Class[_]*) = {
      assert(param.parameterAnnotations.size == pars.size)

      val method = createMethod(b, name, returnType)

      val methodResult = param.parameterAnnotations.zip(pars) match {
        case Nil => method
        case (anns, par) :: rest => rest.foldLeft(method.withParameter(par).annotateParameter(anns: _*))((mbi, t) => {
          val (ianns, ipar) = t
          mbi.withParameter(ipar).annotateParameter(ianns: _*)
        })
      }

      val methodBuilder = methodResult.intercept(MethodDelegation.toInstanceField(param.d.getClass, name))
      addAnnotations(methodBuilder, param.methodAnnotations)
    }

    def build(): Any = {
      val l = loadClass(builder)
      val i = l.newInstance()
      methodMap.foreach { case (str, f) => l.getField(str).set(i, f) }
      i
    }

    private def loadClass(b: Builder[Any]) = b.make.load(ClassLoader.getSystemClassLoader, ClassLoadingStrategy.Default.INJECTION).getLoaded
  }

  object ResouceBuilder {
    def newResouce(className: String, anns: Annotation*) = new ResouceBuilder(new ByteBuddy().subclass(classOf[Any]).name(className).annotateType(anns: _*), Map())
  }


  object EndPoint {

    def annotations(endpointAnnotations: Annotation*): EndPointBuilderZero = new EndPointBuilderZero(endpointAnnotations)


    class EndPointBuilderZero(endpointAnnotations: Seq[Annotation]) {
      def endpoint[T](f: () => T): EndPoint[() => T] = EndPoint(endpointAnnotations, List(), f)

      def firstParameter(annotations: Annotation*) = new EndpointBuilderOne(endpointAnnotations, annotations)
    }

    class EndpointBuilderOne(endpointAnnotations: Seq[Annotation], anns: Seq[Annotation]) {

      def secondParameter(annotations: Annotation*) = new EndpointBuilderTwo(endpointAnnotations, List(anns, annotations))

      def function[A, Z](f: A => Z): EndPoint[A => Z] = EndPoint(endpointAnnotations, List(anns), f)
    }

    class EndpointBuilderTwo(endpointAnnotations: Seq[Annotation], anns: List[Seq[Annotation]]) {
      assert(anns.size == 2)

      def thirdParameter(annotations: Annotation*) = new EndpointBuilderThree(endpointAnnotations, anns :+ annotations)

      def function[A, B, Z](f: (A, B) => Z): EndPoint[(A, B) => Z] = EndPoint(endpointAnnotations, anns, f)
    }

    class EndpointBuilderThree(endpointAnnotations: Seq[Annotation], anns: List[Seq[Annotation]]) {
      assert(anns.size == 3)

      def fourthParameter(annotations: Annotation*) = new EndpointBuilderFour(endpointAnnotations, anns :+ annotations)

      def function[A, B, C, Z](f: (A, B, C) => Z): EndPoint[(A, B, C) => Z] = EndPoint(endpointAnnotations, anns, f)
    }

    class EndpointBuilderFour(endpointAnnotations: Seq[Annotation], anns: List[Seq[Annotation]]) {
      assert(anns.size == 4)

      def function[A, B, C, D, Z](f: (A, B, C, D) => Z): EndPoint[(A, B, C, D) => Z] = EndPoint(endpointAnnotations, anns, f)
    }

  }

  case class EndPoint[T] private(methodAnnotations: Seq[Annotation], parameterAnnotations: List[Seq[Annotation]], d: T)

}
