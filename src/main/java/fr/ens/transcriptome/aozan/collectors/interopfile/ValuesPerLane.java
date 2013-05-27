package fr.ens.transcriptome.aozan.collectors.interopfile;

import java.util.List;

public class ValuesPerLane {

  
  public Number average(List<Number> list) {
    Number value = null;
    Class<?> classValue = list.get(0).getClass();

    if (classValue == Double.class) {
      double sum = 0.0;
      double count = 0;

      for (int i = 0; i < list.size(); i++) {
        Double val = list.get(i).doubleValue();
        if (!val.isInfinite()) {
          sum += val;
          count++;
        }
      }
      value = sum / count;

    } else if (classValue == Float.class) {
      float sum = 0.f;
      float count = 0.f;

      for (int i = 0; i < list.size(); i++) {
        Float val = list.get(i).floatValue();
        if (!val.isInfinite()) {
          sum += val;
          count++;
        }
      }
      value = sum / count;

    } else if (classValue == Integer.class) {
      int sum = 0;
      int count = 0;

      for (int i = 0; i < list.size(); i++) {
        Integer val = list.get(i).intValue();
        if (val != null) {
          sum += val;
          count++;
        }
      }
      value = sum / count;

    } else if (classValue == Long.class) {
      long sum = 0L;
      long count = 0L;

      for (int i = 0; i < list.size(); i++) {
        Long val = list.get(i).longValue();
        if (val != null) {
          sum += val;
          count++;
        }
      }
      value = sum / count;
    }

    return value;
  }

  public double standardDeviation(List<Number> list, Number averageValue) {

    double result = 0.0;
    Class<?> classValue = list.get(0).getClass();

    if (classValue == Double.class) {
      double average = averageValue.doubleValue();
      double sum = 0.0;
      Double val = 0.0;
      double count = 0.0;
      for (int i = 0; i < list.size(); i++) {
        val = list.get(i).doubleValue();
        if (!val.isInfinite()) {
          val -= average;
          sum += val * val;
          count++;
        }
      }
      result = Math.sqrt(sum / count);

    } else if (classValue == Float.class) {
      float average = averageValue.floatValue();
      float sum = 0.f;
      Float val = 0.f;
      float count = 0.f;

      for (int i = 0; i < list.size(); i++) {
        val = list.get(i).floatValue();
        if (!val.isInfinite()) {
          val -= average;
          sum += val * val;
          count++;
        }
      }
      result = Math.sqrt(sum / count);

    } else if (classValue == Integer.class) {
      int average = averageValue.intValue();
      int sum = 0;
      Integer val = 0;
      int count = 0;

      for (int i = 0; i < list.size(); i++) {
        val = list.get(i).intValue();
        if (val != null) {
          val -= average;
          sum += val * val;
          count++;
        }
      }
      result = Math.sqrt(sum / count);

    } else if (classValue == Long.class) {
      long average = averageValue.longValue();
      long sum = 0L;
      Long val = 0L;
      long count = 0L;

      for (int i = 0; i < list.size(); i++) {
        val = list.get(i).longValue();
        if (val != null) {
          val -= average;
          sum += val * val;
          count++;
        }
      }
      result = Math.sqrt(sum / count);
    }

    return result;
  }
}
