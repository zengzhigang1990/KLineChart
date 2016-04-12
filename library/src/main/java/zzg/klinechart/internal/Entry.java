package zzg.klinechart.internal;

/**
 * Class representing one entry in the chart.
 * <p/>
 * Created by 曾志刚 on 16-3-30.
 */
public class Entry {
  public final float high;
  public final float low;
  public final float open;
  public final float close;

  public final int volume;

  public String xValue;

  public Entry(float high, float low, float open, float close, int volume, String xValue) {
    this.high = high;
    this.low = low;
    this.open = open;
    this.close = close;
    this.volume = volume;
    this.xValue = xValue;
  }
}
