package com.devexperts.openhack2022.cell_sandbox.geom

import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sqrt

fun testLineAndCircleIntersection(
    circleCenter: Vector2,
    circleRadius: Double,
    linePoint1: Vector2,
    linePoint2: Vector2
): Set<Vector2> {
    // Googled this solution
    val v1 = linePoint2 - linePoint1
    val v2 = linePoint1 - circleCenter
    val b = -2 * (v1.x * v2.x + v1.y * v2.y)
    val c = 2 * (v1.x.pow(2) + v1.y.pow(2))
    val d = sqrt(b.pow(2) - 2 * c * (v2.x.pow(2) + v2.y.pow(2) - circleRadius.pow(2)))
    if (d.isNaN())
        return emptySet()

    val u1 = (b - d) / c
    val u2 = (b + d) / c
    val result = mutableSetOf<Vector2>()
    if (u1 in 0.0..1.0)
        result += Vector2(linePoint1.x + v1.x * u1, linePoint1.y + v1.y * u1)
    if (u2 in 0.0..1.0)
        result += Vector2(linePoint1.x + v1.x * u2, linePoint1.y + v1.y * u2)
    return result
}

fun testCirclesIntersection(
    circle1Center: Vector2,
    circle1Radius: Double,
    circle2Center: Vector2,
    circle2Radius: Double
): Set<Vector2> {
    val distance = circle1Center.distance(circle2Center)
    if (distance > circle1Radius + circle2Radius)
        return emptySet()
    val a = (circle1Center + circle2Center)*0.5 + (circle2Center - circle1Center)*(circle1Radius.pow(2) - circle2Radius.pow(2))/(2*distance.pow(2))
    val b = Vector2(circle2Center.y - circle1Center.y, circle1Center.x - circle2Center.x) * 0.5*sqrt(2*(circle1Radius.pow(2) + circle2Radius.pow(2))/distance.pow(2) - (circle1Radius.pow(2) - circle2Radius.pow(2)).pow(2)/distance.pow(4) - 1)
    if (a.x.isNaN() || a.y.isNaN() || b.x.isNaN() || b.y.isNaN())
        return emptySet()
    return setOf(a + b, a - b)
}

fun testLinesIntersection(
    lineA: Pair<Vector2, Vector2>,
    lineB: Pair<Vector2, Vector2>
): Vector2? {
    val angle1Ratio = (lineA.second.y - lineA.first.y) / (lineA.second.x - lineA.first.x)
    val angle2Ratio = (lineB.second.y - lineB.first.y) / (lineB.second.x - lineB.first.x)

    if (angle1Ratio.isNaN() || angle2Ratio.isNaN() ||
        (angle1Ratio.isInfinite() && angle2Ratio.isInfinite()) ||
        angle1Ratio == angle2Ratio)
        return null

    if (angle1Ratio.isInfinite())
        return testVerticalLineIntersection(lineA, lineB)
    if (angle2Ratio.isInfinite())
        return testVerticalLineIntersection(lineB, lineA)

    val lineAOffset = lineA.first.y - angle1Ratio * lineA.first.x
    val lineBOffset = lineB.first.y - angle2Ratio * lineB.first.x

    val intersection = Vector2((lineBOffset - lineAOffset) / (angle1Ratio - angle2Ratio), 0)
    intersection.y = angle1Ratio * intersection.x + lineAOffset

    val avgAPoint = (lineA.first + lineA.second)/2
    val lineARect = Vector2(
        abs(lineA.first.x - lineA.second.x),
        abs(lineA.first.y - lineA.second.y)
    )
    val avgBPoint = (lineB.first + lineB.second)/2
    val lineBRect = Vector2(
        abs(lineB.first.x - lineB.second.x),
        abs(lineB.first.y - lineB.second.y)
    )
    return if (
        abs(intersection.x - avgAPoint.x) <= lineARect.x/2 &&
        abs(intersection.y - avgAPoint.y) <= lineARect.y/2 &&
        abs(intersection.x - avgBPoint.x) <= lineBRect.x/2 &&
        abs(intersection.y - avgBPoint.y) <= lineBRect.y/2
    ) intersection else null
}

fun projectPointOnLine(point: Vector2, line: Pair<Vector2, Vector2>): Vector2 {
    val a = line.first
    val b = line.second
    if (a.x == b.x)
        return Vector2(a.x, point.y)
    return a + a.to(b) * a.to(point).dot(a.to(b)) / a.to(b).dot(a.to(b))
}

private fun testVerticalLineIntersection(
    verticalLine: Pair<Vector2, Vector2>,
    otherLine: Pair<Vector2, Vector2>
): Vector2? {
    if (otherLine.first.x == otherLine.second.x)
        return null
    val otherAngleRatio = (otherLine.second.y - otherLine.first.y) / (otherLine.second.x - otherLine.first.x)
    val line2Offset = otherLine.first.y - otherAngleRatio * otherLine.first.x
    val intersection = Vector2(verticalLine.first.x, 0)
    intersection.y = otherAngleRatio * intersection.x  + line2Offset
    if (otherLine.first.x < intersection.x && otherLine.second.x < intersection.x ||
        otherLine.first.x > intersection.x && otherLine.second.x > intersection.x)
        return null
    val avgY = (verticalLine.first.y + verticalLine.second.y)/2
    val height = abs(verticalLine.first.y - verticalLine.second.y)
    return if (abs(intersection.y - avgY) <= height/2) intersection else null
}
