package cgeo.geocaching.filters.core;

import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.location.GeopointFormatter;
import cgeo.geocaching.location.GeopointParser;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.sensors.LocationDataProvider;
import cgeo.geocaching.storage.DataStore;
import cgeo.geocaching.storage.SqlBuilder;
import cgeo.geocaching.utils.JsonUtils;
import cgeo.geocaching.utils.config.LegacyConfig;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.commons.lang3.BooleanUtils;

public class DistanceGeocacheFilter extends NumberRangeGeocacheFilter<Float> {

    private static final String CONFIG_KEY_COORD = "coord";
    private static final String CONFIG_KEY_USE_CURRENT_POS = "use_current_pos";

    private Geopoint coordinate;
    private boolean useCurrentPosition;

    public DistanceGeocacheFilter() {
        super(Float::valueOf, f -> f);
    }

    /**
     * Gets fixed-value coordinate set to this filter, may be null
     */
    @Nullable
    public Geopoint getCoordinate() {
        return coordinate;
    }

    public void setCoordinate(final Geopoint coordinate) {
        this.coordinate = coordinate;
    }

    public boolean isUseCurrentPosition() {
        return useCurrentPosition;
    }

    public void setUseCurrentPosition(final boolean useCurrentPosition) {
        this.useCurrentPosition = useCurrentPosition;
    }

    @Override
    protected Float getValue(final Geocache cache) {
        final Geopoint gp = (useCurrentPosition || coordinate == null) ?
                LocationDataProvider.getInstance().currentGeo().getCoords() : coordinate;

        return gp.distanceTo(cache.getCoords());
    }

    /**
     * Returns the coordinate which will effectively be used for calculation (either fixed-value or current position)
     */
    @NonNull
    public Geopoint getEffectiveCoordinate() {
        return (useCurrentPosition || coordinate == null) ?
                LocationDataProvider.getInstance().currentGeo().getCoords() : coordinate;
    }

    @Override
    public void setConfig(final LegacyConfig config) {
        super.setConfig(config);
        useCurrentPosition = config.getFirstValue(CONFIG_KEY_USE_CURRENT_POS, false, BooleanUtils::toBoolean);
        coordinate = config.getFirstValue(CONFIG_KEY_COORD, null, s -> GeopointParser.parse(s, null));
    }

    @Override
    public LegacyConfig getConfig() {
        final LegacyConfig config = super.getConfig();
        config.putList(CONFIG_KEY_USE_CURRENT_POS, Boolean.toString(useCurrentPosition));
        config.putList(CONFIG_KEY_COORD, coordinate == null ? "-" : GeopointFormatter.format(GeopointFormatter.Format.LAT_LON_DECMINUTE, coordinate));
        return config;
    }

    @Nullable
    @Override
    public ObjectNode getJsonConfig() {
        final ObjectNode node = super.getJsonConfig();
        JsonUtils.setBoolean(node, CONFIG_KEY_USE_CURRENT_POS, useCurrentPosition);
        JsonUtils.setText(node, CONFIG_KEY_COORD, coordinate == null ? null : GeopointFormatter.format(GeopointFormatter.Format.LAT_LON_DECMINUTE, coordinate));
        return node;
    }

    @Override
    public void setJsonConfig(@NonNull final ObjectNode config) {
        super.setJsonConfig(config);
        useCurrentPosition = JsonUtils.getBoolean(config, CONFIG_KEY_USE_CURRENT_POS, false);
        final String coordText = JsonUtils.getText(config, CONFIG_KEY_COORD, null);
        coordinate = coordText == null ? null : GeopointParser.parse(coordText, null);
    }

    @Override
    public void addToSql(final SqlBuilder sqlBuilder) {
        final Geopoint gp = (useCurrentPosition || coordinate == null) ?
                LocationDataProvider.getInstance().currentGeo().getCoords() : coordinate;
        final String sql = DataStore.getSqlDistanceSquare(
                sqlBuilder.getMainTableId() + ".latitude", sqlBuilder.getMainTableId() + ".longitude", gp);

        addRangeToSqlBuilder(sqlBuilder, sql, v -> v * v);
    }

    @Override
    public boolean isFiltering() {
        return super.isFiltering() || !useCurrentPosition;
    }

    @Override
    protected String getUserDisplayableConfig() {
        return GeopointFormatter.format(GeopointFormatter.Format.LAT_LON_DECMINUTE_SHORT, getEffectiveCoordinate()) + "(" + super.getUserDisplayableConfig() + ")";
    }


}
