package cgeo.geocaching.models.geoitem;

import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.location.Viewport;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/** Represents a drawable primitive GeoItem such as a point, polyline or polygon */
public class GeoPrimitive implements GeoItem, Parcelable {

    //immutable
    @NonNull private final GeoItem.GeoType type;
    @NonNull private final List<Geopoint> points;
    @Nullable private final GeoIcon icon;
    private final float radius;
    @Nullable private final GeoStyle style;

    //lazy-calculated
    private Viewport viewport;

    private GeoPrimitive(@Nullable final GeoItem.GeoType type, @NonNull final List<Geopoint> points, @Nullable final GeoIcon icon, final float radius, @Nullable final GeoStyle style) {
        this.type = type == null || type == GeoType.GROUP ? GeoItem.GeoType.POLYLINE : type;
        this.points = Collections.unmodifiableList(points);
        this.icon = icon;
        this.radius = radius;
        this.style = style;
    }

    @NonNull
    @Override
    public GeoItem.GeoType getType() {
        return type;
    }

    @NonNull
    public List<Geopoint> getPoints() {
        return points;
    }

    @Nullable
    public GeoIcon getIcon() {
        return icon;
    }

    public float getRadius() {
        return radius;
    }

    @Nullable
    public GeoStyle getStyle() {
        return style;
    }

    @Nullable
    public Viewport getViewport() {
        if (viewport == null) {
            viewport = Viewport.containing(this.points);
        }
        return viewport;
    }

    @Nullable
    @Override
    public Geopoint getCenter() {
        //shortcut for markers
        if (getPoints().size() == 1) {
            return getPoints().get(0);
        }
        final Viewport vp = getViewport();
        return vp == null ? null : vp.getCenter();
    }

    @Override
    public boolean isValid() {

        if (getIcon() != null && getCenter() == null) {
            return false;
        }

        switch (getType()) {
            case GROUP:
                return false;
            case MARKER:
                return getCenter() != null && getCenter().isValid();
            case CIRCLE:
                return getCenter() != null && getCenter().isValid() && getRadius() > 0;
            case POLYGON:
            case POLYLINE:
            default:
                return getPoints().size() >= 2;
        }
    }

    public boolean intersects(final Viewport box, final float yPerLat, final float xPerLon) {
        switch (getType()) {
            case MARKER:
                if (box.contains(getPoints().get(0))) {
                    return true;
                }
                break;
            default:
                //do nothing
                break;
        }

        //handle icon
        if (yPerLat > 0 && xPerLon > 0 && icon != null) {
            final Viewport iconViewport = icon.getViewport(getPoints().get(0), yPerLat, xPerLon);
            if (iconViewport != null && iconViewport.intersects(box)) {
                return true;
            }
        }

        return false;
    }


    public Builder buildUpon() {
        return new Builder().setType(type).addPoints(points).setIcon(icon).setRadius(radius).setStyle(style);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static GeoPrimitive createPoint(final Geopoint p, final GeoStyle style) {
        return createCircle(p, 0.1f, style);
    }

    public static GeoPrimitive createMarker(final Geopoint p, final GeoIcon icon) {
        return new Builder().setType(GeoItem.GeoType.MARKER).addPoints(p).setIcon(icon).build();
    }

    public static GeoPrimitive createCircle(final Geopoint p, final float radiusKm, final GeoStyle style) {
        return new Builder().setType(GeoItem.GeoType.CIRCLE).addPoints(p).setRadius(radiusKm).setStyle(style).build();
    }

    public static GeoPrimitive createPolyline(final Collection<Geopoint> p, final GeoStyle style) {
        return new Builder().setType(GeoItem.GeoType.POLYLINE).addPoints(p).setStyle(style).build();
    }

    public static GeoPrimitive createPolygon(final Collection<Geopoint> p, final GeoStyle style) {
        return new Builder().setType(GeoItem.GeoType.POLYGON).addPoints(p).setStyle(style).build();
    }

    //equals/HashCode

    @Override
    public boolean equals(final Object o) {
        if (!(o instanceof GeoPrimitive)) {
            return false;
        }
        final GeoPrimitive other = (GeoPrimitive) o;
        return
            Objects.equals(type, other.type) &&
            Objects.equals(points, other.points) &&
            Objects.equals(icon, other.icon) &&
            radius == other.radius &&
            Objects.equals(style, other.style);
    }

    @Override
    public int hashCode() {
        return type.ordinal() * 13 + (points.isEmpty() || points.get(0) == null ? 7 : points.get(0).hashCode());
    }

    @NonNull
    @Override
    public String toString() {
        return getType() + ": " + getPoints().size() + "points in " + getViewport();
    }

    //implements Builder

    public static class Builder {
        private GeoItem.GeoType type;
        private final List<Geopoint> points = new ArrayList<>();
        private GeoIcon icon;
        private float radius;
        private GeoStyle style;

        private Builder() {
            //no free instantiation
        }

        public Builder setType(final GeoItem.GeoType type) {
            this.type = type;
            return this;
        }

        public Builder addPoints(final Collection<Geopoint> gps) {
            points.addAll(gps);
            return this;
        }

        public Builder addPoints(final Geopoint ... gps) {
            points.addAll(Arrays.asList(gps));
            return this;
        }

        public Builder setIcon(final GeoIcon icon) {
            this.icon = icon;
            return this;
        }

        public Builder setRadius(final float radius) {
            this.radius = radius;
            return this;
        }

        public Builder setStyle(final GeoStyle style) {
            this.style = style;
            return this;
        }

        public GeoPrimitive build() {
            return new GeoPrimitive(type, points, icon, radius, style);
        }

    }


    // implements Parcelable

    protected GeoPrimitive(final Parcel in) {
        type = GeoItem.GeoType.values()[in.readInt()];
        final List<Geopoint> pointsReadWrite = new ArrayList<>();
        in.readList(pointsReadWrite, Geopoint.class.getClassLoader());
        points = Collections.unmodifiableList(pointsReadWrite);
        icon = in.readParcelable(GeoIcon.class.getClassLoader());
        radius = in.readFloat();
        style = in.readParcelable(GeoStyle.class.getClassLoader());
    }

    public static final Creator<GeoPrimitive> CREATOR = new Creator<GeoPrimitive>() {
        @Override
        public GeoPrimitive createFromParcel(final Parcel in) {
            return new GeoPrimitive(in);
        }

        @Override
        public GeoPrimitive[] newArray(final int size) {
            return new GeoPrimitive[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(final Parcel dest, final int flags) {
        dest.writeInt(type.ordinal());
        dest.writeList(points);
        dest.writeParcelable(icon, flags);
        dest.writeFloat(radius);
        dest.writeParcelable(style, flags);
    }



}