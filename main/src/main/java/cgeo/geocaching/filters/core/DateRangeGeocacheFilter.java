package cgeo.geocaching.filters.core;

import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.storage.SqlBuilder;
import cgeo.geocaching.utils.config.LegacyConfig;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Date;

import com.fasterxml.jackson.databind.node.ObjectNode;


public abstract class DateRangeGeocacheFilter extends BaseGeocacheFilter {

    private final DateFilter dateFilter = new DateFilter();

    protected abstract Date getDate(Geocache cache);

    protected String getSqlColumnName() {
        return null;
    }

    @Override
    public Boolean filter(final Geocache cache) {
        return dateFilter.matches(getDate(cache));
    }

    public Date getMinDate() {
        return dateFilter.getMinDate();
    }

    public DateFilter getDateFilter() {
        return dateFilter;
    }

    public Date getMaxDate() {
        return dateFilter.getMaxDate();
    }

    public void setMinMaxDate(final Date min, final Date max) {
        this.dateFilter.setMinMaxDate(min, max);
    }

    public void setRelativeMinMaxDays(final int minOffset, final int maxOffset) {
        this.dateFilter.setRelativeDays(minOffset, maxOffset);
    }

    public int getDaysSinceMinDate() {
        return dateFilter.getDaysSinceMinDate();
    }

    @Override
    public void setConfig(final LegacyConfig config) {
        dateFilter.setConfig(config.get(null));
    }

    @Override
    public LegacyConfig getConfig() {
        final LegacyConfig config = new LegacyConfig();
        config.put(null, dateFilter.getConfig());
        return config;
    }

    @Nullable
    @Override
    public ObjectNode getJsonConfig() {
        return dateFilter.getJsonConfig();
    }

    @Override
    public void setJsonConfig(@NonNull final ObjectNode node) {
        dateFilter.setJsonConfig(node);
    }

    @Override
    public boolean isFiltering() {
        return dateFilter.isFilled();
    }

    @Override
    public void addToSql(final SqlBuilder sqlBuilder) {
        addToSql(sqlBuilder, getSqlColumnName() == null ? null : sqlBuilder.getMainTableId() + "." + getSqlColumnName());
    }

    protected void addToSql(final SqlBuilder sqlBuilder, final String valueExpression) {
        dateFilter.addToSql(sqlBuilder, valueExpression);
    }

    @Override
    protected String getUserDisplayableConfig() {
        return dateFilter.getUserDisplayableConfig();
    }

}
