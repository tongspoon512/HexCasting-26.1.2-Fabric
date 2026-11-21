package at.petrak.hexcasting.api.casting.area;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * The immutable geometry of a domain.
 * <p>
 * Polygon-shaped domains (pentagram and triangle) are prisms: the polygon itself is the top face and
 * the domain extends {@link #POLYGON_DEPTH} blocks downwards from it. Sphere and cuboid domains are
 * ordinary volumes.
 */
public final class AreaShape {
    /** How far polygon-shaped domains extend below their polygon. */
    public static final double POLYGON_DEPTH = 10.0;
    public static final double MAX_SPHERE_RADIUS = 64.0;

    private static final double EPS = 1e-6;

    private final AreaKind kind;
    private final List<Vec3> points;
    private final double radius;
    private final List<Vec3> outline;

    private AreaShape(AreaKind kind, List<Vec3> points, double radius) {
        this.kind = kind;
        this.points = List.copyOf(points);
        this.radius = radius;
        this.outline = kind.isPolygon() ? computeHull(this.points) : this.points;
    }

    public static AreaShape pentagram(List<Vec3> points) {
        if (points.size() != 5) {
            throw new IllegalArgumentException("a pentagram needs exactly 5 points");
        }
        return new AreaShape(AreaKind.PENTAGRAM, points, 0.0);
    }

    public static AreaShape triangle(List<Vec3> points) {
        if (points.size() != 3) {
            throw new IllegalArgumentException("a triangle needs exactly 3 points");
        }
        return new AreaShape(AreaKind.TRIANGLE, points, 0.0);
    }

    public static AreaShape sphere(Vec3 center, double radius) {
        return new AreaShape(AreaKind.SPHERE, List.of(center), radius);
    }

    public static AreaShape cuboid(Vec3 cornerA, Vec3 cornerB) {
        return new AreaShape(AreaKind.CUBOID, List.of(cornerA, cornerB), 0.0);
    }

    public AreaKind getKind() {
        return this.kind;
    }

    /** The points exactly as they were given by the caster. */
    public List<Vec3> getPoints() {
        return this.points;
    }

    /** The convex hull of the polygon points, used for containment tests. */
    public List<Vec3> getOutline() {
        return this.outline;
    }

    public double getRadius() {
        return this.radius;
    }

    public Vec3 getCenter() {
        if (this.kind == AreaKind.SPHERE) {
            return this.points.get(0);
        }
        if (this.kind == AreaKind.CUBOID) {
            return this.points.get(0).add(this.points.get(1)).scale(0.5);
        }
        Vec3 sum = Vec3.ZERO;
        for (Vec3 point : this.outline) {
            sum = sum.add(point);
        }
        return sum.scale(1.0 / this.outline.size());
    }

    /**
     * The direction polygon domains are extruded towards. It is always the downward-facing normal,
     * so "down 10 blocks" behaves the same no matter in which order the caster supplied the points.
     */
    public Vec3 getExtrusionDirection() {
        Vec3 normal = newellNormal(this.outline);
        if (normal.lengthSqr() < EPS * EPS) {
            return Vec3.ZERO;
        }
        return normal.y > 0 ? normal.scale(-1.0) : normal;
    }

    /** The surface area of the domain, in square blocks. */
    public double area() {
        return switch (this.kind) {
            case SPHERE -> 4.0 * Math.PI * this.radius * this.radius;
            case CUBOID -> {
                Vec3 a = this.points.get(0);
                Vec3 b = this.points.get(1);
                double dx = Math.abs(a.x - b.x);
                double dy = Math.abs(a.y - b.y);
                double dz = Math.abs(a.z - b.z);
                yield 2.0 * (dx * dy + dy * dz + dz * dx);
            }
            default -> polygonArea();
        };
    }

    public boolean isDegenerate() {
        return switch (this.kind) {
            case SPHERE -> this.radius < 0.5 || this.radius > MAX_SPHERE_RADIUS;
            case CUBOID -> {
                Vec3 a = this.points.get(0);
                Vec3 b = this.points.get(1);
                yield Math.abs(a.x - b.x) < 0.5
                    || Math.abs(a.y - b.y) < 0.5
                    || Math.abs(a.z - b.z) < 0.5;
            }
            default -> this.outline.size() < 3 || polygonArea() < 0.5;
        };
    }

    public boolean contains(Vec3 point) {
        return switch (this.kind) {
            case SPHERE -> point.distanceToSqr(this.points.get(0)) <= this.radius * this.radius + EPS;
            case CUBOID -> {
                Vec3 a = this.points.get(0);
                Vec3 b = this.points.get(1);
                double minX = Math.min(a.x, b.x) - EPS;
                double minY = Math.min(a.y, b.y) - EPS;
                double minZ = Math.min(a.z, b.z) - EPS;
                double maxX = Math.max(a.x, b.x) + EPS;
                double maxY = Math.max(a.y, b.y) + EPS;
                double maxZ = Math.max(a.z, b.z) + EPS;
                yield point.x >= minX && point.x <= maxX
                    && point.y >= minY && point.y <= maxY
                    && point.z >= minZ && point.z <= maxZ;
            }
            default -> containsInPolygon(point);
        };
    }

    private boolean containsInPolygon(Vec3 point) {
        if (this.outline.size() < 3) {
            return false;
        }
        Vec3 normal = this.getExtrusionDirection();
        if (normal.lengthSqr() < EPS * EPS) {
            return false;
        }
        Vec3 origin = this.outline.get(0);
        double depth = point.subtract(origin).dot(normal);
        if (depth < -EPS || depth > POLYGON_DEPTH + EPS) {
            return false;
        }
        Vec3 u = basisU(normal);
        Vec3 v = normal.cross(u).normalize();
        return pointInConvexPolygon(this.outline, origin, u, v, point);
    }

    private double polygonArea() {
        if (this.outline.size() < 3) {
            return 0.0;
        }
        Vec3 normal = newellNormal(this.outline);
        if (normal.lengthSqr() < EPS * EPS) {
            return 0.0;
        }
        Vec3 u = basisU(normal);
        Vec3 v = normal.cross(u).normalize();
        Vec3 origin = this.outline.get(0);
        double sum = 0.0;
        for (int i = 0; i < this.outline.size(); i++) {
            Vec3 a = this.outline.get(i).subtract(origin);
            Vec3 b = this.outline.get((i + 1) % this.outline.size()).subtract(origin);
            sum += a.dot(u) * b.dot(v) - b.dot(u) * a.dot(v);
        }
        return Math.abs(sum) * 0.5;
    }

    public String display() {
        StringBuilder sb = new StringBuilder("delta");
        for (Vec3 point : this.points) {
            sb.append('(').append(format(point.x)).append(',').append(format(point.y)).append(',')
                .append(format(point.z)).append(')');
        }
        if (this.kind == AreaKind.SPHERE) {
            sb.append('(').append(format(this.radius)).append(')');
        }
        return sb.toString();
    }

    public CompoundTag serialize() {
        CompoundTag tag = new CompoundTag();
        tag.putString("kind", this.kind.getSerializedName());
        ListTag pointsTag = new ListTag();
        for (Vec3 point : this.points) {
            CompoundTag pointTag = new CompoundTag();
            pointTag.putDouble("x", point.x);
            pointTag.putDouble("y", point.y);
            pointTag.putDouble("z", point.z);
            pointsTag.add(pointTag);
        }
        tag.put("points", pointsTag);
        tag.putDouble("radius", this.radius);
        return tag;
    }

    public static AreaShape deserialize(CompoundTag tag) {
        AreaKind kind = AreaKind.byName(tag.getStringOr("kind", ""));
        ListTag pointsTag = tag.getListOrEmpty("points");
        List<Vec3> points = new ArrayList<>(pointsTag.size());
        for (int i = 0; i < pointsTag.size(); i++) {
            CompoundTag pointTag = pointsTag.getCompoundOrEmpty(i);
            points.add(new Vec3(
                pointTag.getDoubleOr("x", 0.0),
                pointTag.getDoubleOr("y", 0.0),
                pointTag.getDoubleOr("z", 0.0)));
        }
        double radius = tag.getDoubleOr("radius", 0.0);
        return switch (kind) {
            case PENTAGRAM -> pentagram(points);
            case TRIANGLE -> triangle(points);
            case SPHERE -> {
                if (points.size() != 1) {
                    throw new IllegalArgumentException("a sphere needs exactly 1 center");
                }
                yield sphere(points.get(0), radius);
            }
            case CUBOID -> {
                if (points.size() != 2) {
                    throw new IllegalArgumentException("a cuboid needs exactly 2 corners");
                }
                yield cuboid(points.get(0), points.get(1));
            }
        };
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof AreaShape shape)) {
            return false;
        }
        return this.kind == shape.kind
            && Double.compare(this.radius, shape.radius) == 0
            && this.points.equals(shape.points);
    }

    @Override
    public int hashCode() {
        return this.points.hashCode() * 31 + this.kind.hashCode() * 17 + Double.hashCode(this.radius);
    }

    // --- geometry helpers ---

    private static String format(double value) {
        if (Math.abs(value - Math.rint(value)) < 1e-6) {
            return Long.toString(Math.round(value));
        }
        String text = String.format(Locale.ROOT, "%.2f", value);
        while (text.endsWith("0")) {
            text = text.substring(0, text.length() - 1);
        }
        if (text.endsWith(".")) {
            text = text.substring(0, text.length() - 1);
        }
        return text;
    }

    private static Vec3 newellNormal(List<Vec3> points) {
        if (points.size() < 3) {
            return Vec3.ZERO;
        }
        double x = 0.0;
        double y = 0.0;
        double z = 0.0;
        for (int i = 0; i < points.size(); i++) {
            Vec3 a = points.get(i);
            Vec3 b = points.get((i + 1) % points.size());
            x += (a.y - b.y) * (a.z + b.z);
            y += (a.z - b.z) * (a.x + b.x);
            z += (a.x - b.x) * (a.y + b.y);
        }
        Vec3 normal = new Vec3(x, y, z);
        return normal.lengthSqr() < EPS * EPS ? Vec3.ZERO : normal.normalize();
    }

    private static Vec3 basisU(Vec3 normal) {
        Vec3 axis = Math.abs(normal.x) < 0.9 ? new Vec3(1.0, 0.0, 0.0) : new Vec3(0.0, 0.0, 1.0);
        return normal.cross(axis).normalize();
    }

    private static boolean pointInConvexPolygon(List<Vec3> hull, Vec3 origin, Vec3 u, Vec3 v, Vec3 point) {
        Vec3 relative = point.subtract(origin);
        double px = relative.dot(u);
        double py = relative.dot(v);
        int sign = 0;
        for (int i = 0; i < hull.size(); i++) {
            Vec3 a = hull.get(i).subtract(origin);
            Vec3 b = hull.get((i + 1) % hull.size()).subtract(origin);
            double ax = a.dot(u);
            double ay = a.dot(v);
            double bx = b.dot(u);
            double by = b.dot(v);
            double cross = (bx - ax) * (py - ay) - (by - ay) * (px - ax);
            if (Math.abs(cross) <= EPS) {
                continue;
            }
            int current = cross > 0 ? 1 : -1;
            if (sign == 0) {
                sign = current;
            } else if (sign != current) {
                return false;
            }
        }
        return true;
    }

    private static List<Vec3> computeHull(List<Vec3> points) {
        if (points.size() < 3) {
            return points;
        }
        Vec3 normal = newellNormal(points);
        if (normal.lengthSqr() < EPS * EPS) {
            return points;
        }
        Vec3 u = basisU(normal);
        Vec3 v = normal.cross(u).normalize();
        Vec3 origin = points.get(0);
        List<double[]> projected = new ArrayList<>(points.size());
        for (int i = 0; i < points.size(); i++) {
            Vec3 relative = points.get(i).subtract(origin);
            projected.add(new double[]{relative.dot(u), relative.dot(v), i});
        }
        projected.sort(Comparator.<double[]>comparingDouble(p -> p[0]).thenComparingDouble(p -> p[1]));

        List<double[]> unique = new ArrayList<>(projected.size());
        for (double[] point : projected) {
            if (unique.isEmpty()) {
                unique.add(point);
                continue;
            }
            double[] previous = unique.get(unique.size() - 1);
            if (Math.abs(previous[0] - point[0]) > EPS || Math.abs(previous[1] - point[1]) > EPS) {
                unique.add(point);
            }
        }
        if (unique.size() < 3) {
            return points;
        }

        List<double[]> hull = new ArrayList<>();
        for (double[] point : unique) {
            while (hull.size() >= 2 && cross(hull.get(hull.size() - 2), hull.get(hull.size() - 1), point) <= 0) {
                hull.remove(hull.size() - 1);
            }
            hull.add(point);
        }
        int lowerSize = hull.size() + 1;
        for (int i = unique.size() - 2; i >= 0; i--) {
            double[] point = unique.get(i);
            while (hull.size() >= lowerSize
                && cross(hull.get(hull.size() - 2), hull.get(hull.size() - 1), point) <= 0) {
                hull.remove(hull.size() - 1);
            }
            hull.add(point);
        }
        if (!hull.isEmpty()) {
            hull.remove(hull.size() - 1);
        }
        if (hull.size() < 3) {
            return points;
        }
        List<Vec3> result = new ArrayList<>(hull.size());
        for (double[] point : hull) {
            result.add(points.get((int) point[2]));
        }
        return result;
    }

    private static double cross(double[] origin, double[] a, double[] b) {
        return (a[0] - origin[0]) * (b[1] - origin[1]) - (a[1] - origin[1]) * (b[0] - origin[0]);
    }
}
