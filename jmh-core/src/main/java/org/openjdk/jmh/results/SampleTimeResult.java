/*
 * Copyright (c) 2005, 2013, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package org.openjdk.jmh.results;

import org.openjdk.jmh.runner.options.TimeValue;
import org.openjdk.jmh.util.SampleBuffer;
import org.openjdk.jmh.util.Statistics;

import java.util.Collection;
import java.util.concurrent.TimeUnit;

/**
 * Result class that samples operation time.
 */
public class SampleTimeResult extends Result<SampleTimeResult> {
    private static final long serialVersionUID = -295298353763294757L;

    private final SampleBuffer buffer;
    private final TimeUnit outputTimeUnit;

    public SampleTimeResult(ResultRole role, String label, SampleBuffer buffer, TimeUnit outputTimeUnit) {
        this(role, label,
                buffer,
                TimeValue.tuToString(outputTimeUnit) + "/op",
                outputTimeUnit);
    }

    SampleTimeResult(ResultRole role, String label, SampleBuffer buffer, String unit, TimeUnit outputTimeUnit) {
        super(role, label,
                of(buffer, outputTimeUnit),
                unit,
                AggregationPolicy.AVG);
        this.buffer = buffer;
        this.outputTimeUnit = outputTimeUnit;
    }

    private static Statistics of(SampleBuffer buffer, TimeUnit outputTimeUnit) {
        double tuMultiplier = 1.0D * outputTimeUnit.convert(1, TimeUnit.DAYS) / TimeUnit.NANOSECONDS.convert(1, TimeUnit.DAYS);
        return buffer.getStatistics(tuMultiplier);
    }

    @Override
    public String toString() {
        Statistics stats = getStatistics();

        StringBuilder sb = new StringBuilder();
        sb.append("n = ").append(stats.getN()).append(", ");
        sb.append(String.format("mean = %.0f %s",
                stats.getMean(),
                getScoreUnit()));
        sb.append(String.format(", p{0.00, 0.50, 0.90, 0.95, 0.99, 0.999, 0.9999, 1.00} = %.0f, %.0f, %.0f, %.0f, %.0f, %.0f, %.0f, %.0f %s",
                stats.getPercentile(0),
                stats.getPercentile(50),
                stats.getPercentile(90),
                stats.getPercentile(95),
                stats.getPercentile(99),
                stats.getPercentile(99.9),
                stats.getPercentile(99.99),
                stats.getPercentile(100),
                getScoreUnit()));
        return sb.toString();
    }

    @Override
    public String extendedInfo() {
        return simpleExtendedInfo() + percentileExtendedInfo();
    }

    @Override
    protected Aggregator<SampleTimeResult> getThreadAggregator() {
        return new JoiningAggregator();
    }

    @Override
    protected Aggregator<SampleTimeResult> getIterationAggregator() {
        return new JoiningAggregator();
    }

    /**
     * Always add up all the samples into final result.
     * This will allow aggregate result to achieve better accuracy.
     */
    static class JoiningAggregator implements Aggregator<SampleTimeResult> {

        @Override
        public SampleTimeResult aggregate(Collection<SampleTimeResult> results) {
            SampleBuffer buffer = new SampleBuffer();
            TimeUnit tu = null;
            for (SampleTimeResult r : results) {
                buffer.addAll(r.buffer);
                tu = r.outputTimeUnit;
            }
            return new SampleTimeResult(
                    AggregatorUtils.aggregateRoles(results),
                    AggregatorUtils.aggregateLabels(results),
                    buffer,
                    AggregatorUtils.aggregateUnits(results),
                    tu
            );
        }
    }

}

