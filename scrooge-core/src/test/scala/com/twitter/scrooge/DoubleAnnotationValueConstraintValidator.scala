package com.twitter.scrooge

import com.twitter.scrooge.validation.ThriftConstraintValidator

object DoubleAnnotationValueConstraintValidator extends ThriftConstraintValidator[Double, Double] {

  /**
   * The IDL annotation for this constraint validator is validation.successRate = "99.97"
   * where the annotation value is an integer.
   */
  override def annotationClazz: Class[Double] = classOf[Double]

  override def violationMessage(
    obj: Double,
    annotation: Double
  ): String = s"$obj is not greater than or equal to $annotation."

  /** return true if `obj` >= 99.97 */
  override def isValid(
    obj: Double,
    annotation: Double
  ): Boolean = obj >= annotation
}
