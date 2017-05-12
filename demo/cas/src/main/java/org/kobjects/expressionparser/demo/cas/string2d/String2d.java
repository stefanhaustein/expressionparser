package org.kobjects.expressionparser.demo.cas.string2d;

import java.util.ArrayList;

public class String2d {
  private int baseline;
  private String[] lines;

  public enum HorizontalAlign{LEFT, CENTER, RIGHT};

  public static String2d embrace(char open, String2d content, char close) {
    return concat(
        vline(content.height(), content.baseline(), open),
        content,
        vline(content.height(), content.baseline(), close));
  }

  public static String2d vline(int height, int base, char single) {
    if (height == 1) {
      return concat("" + single);
    }
    String[] lines = new String[height];
    String replacement =
        single == '(' ? "\u239b\u239c\u239d" :
        single == ')' ? "\u239e\u239f\u23a0" :
            null;
    lines[0] = String.valueOf(replacement == null ? single : replacement.charAt(0));
    if (height > 2) {
      String middle = String.valueOf(replacement == null ? single : replacement.charAt(1));
      for (int i = 1; i < height - 1; i++) {
        lines[i] = middle;
      }
    }
    lines[height - 1] = String.valueOf(replacement == null ? single : replacement.charAt(2));
    return new String2d(base, lines);
  }

  public static String hline(int width) {
    StringBuilder sb = new StringBuilder(width);
    for (int i = 0; i < width; i++) {
      sb.append('\u2500');  // 23af
    }
    return sb.toString();
  }

  private static String space(int length) {
    StringBuilder sb = new StringBuilder(length);
    for (int i = 0; i < length; i++) {
      sb.append(' ');
    }
    return sb.toString();
  }

  private static String align(String s, int length, HorizontalAlign align) {
    if (s.length() >= length) {
      return s;
    }
    int front = align == HorizontalAlign.LEFT ? 0 : (length - s.length())
        / (align == HorizontalAlign.CENTER ? 2 : 1);
    StringBuilder sb = new StringBuilder(length);
    while (sb.length() < front) {
      sb.append(' ');
    }
    sb.append(s);
    while (sb.length() < length) {
      sb.append(' ');
    }
    return sb.toString();
  }

  public static String2d valueOf(Object o) {
    return new String2d(0, o.toString());
  }

  public static String2d concat(Object... list) {
    int baseline = 0;  // index of the baseline
    int descent = 0;   // number of additional line below baseline
    ArrayList<Object> normalized = new ArrayList<>(list.length);

    for (Object current: list) {
      if (current instanceof String2d.Builder) {
        current = ((String2d.Builder) current).build();
      }
      if (current instanceof String2d) {
        String2d s2d = (String2d) current;
        baseline = Math.max(baseline, s2d.baseline);
        descent = Math.max(descent, s2d.height() - s2d.baseline - 1);
      } else if (!(current instanceof String)) {
        current = String.valueOf(current);
      }
      normalized.add(current);
    }

    StringBuilder[] sb = new StringBuilder[baseline + descent + 1];
    for (int i = 0; i < sb.length; i++) {
      sb[i] = new StringBuilder();
    }

    for (Object current: normalized) {
      String space;
      int filledTo;
      if (current instanceof String2d) {
        String2d s2d = (String2d) current;
        space = space(s2d.width());
        int offset = baseline - s2d.baseline;
        for (int i = 0; i < baseline - s2d.baseline; i++) {
          sb[i].append(space);
        }
        for (int i = 0; i < s2d.height(); i++) {
          sb[i + offset].append(s2d.lines[i]);
        }
        filledTo = offset + s2d.height();
      } else {
        String s = (String) current;
        space = space(s.length());
        for (int i = 0; i < baseline; i++) {
          sb[i].append(space);
        }
        sb[baseline].append(s);
        filledTo = baseline + 1;
      }
      for (int i = filledTo; i < sb.length; i++) {
        sb[i].append(space);
      }
    }
    String[] lines = new String[sb.length];
    for (int i = 0; i < lines.length; i++) {
      lines[i] = sb[i].toString();
    }
    return new String2d(baseline, lines);
  }

  public static String2d stack(HorizontalAlign align, int centerIndex, Object... list) {
    int baseline = 0;
    int width = 0;
    for (int i = 0; i < list.length; i++) {
      if (list[i] instanceof String2d) {
        String2d current = (String2d) list[i];
        if (i < centerIndex) {
          baseline += current.height();
        } else if (i == centerIndex) {
          baseline += current.baseline;
        }
        width = Math.max(width, current.width());
      } else {
        if (i < centerIndex) {
          baseline++;
        }
        width = Math.max(width, String.valueOf(list[i]).length());
      }
    }
    ArrayList<String> lines = new ArrayList<>();
    for (Object current: list) {
      if (current instanceof String2d) {
        for (String s : ((String2d) current).lines) {
          lines.add(align(s, width, align));
        }
      } else {
        lines.add(align(String.valueOf(current), width, align));
      }
    }
    return new String2d(baseline, lines.toArray(new String[lines.size()]));
  }

  private String2d(int baseline, String... lines) {
    this.baseline = baseline;
    this.lines = lines;
  }

  public int baseline() {
    return baseline;
  }

  public int height() {
    return lines.length;
  }

  public int width() {
    return lines.length > 0 ? lines[0].length() : 0;
  }

  public String toString() {
    int count = lines.length;
    if (count <= 1) {
      return count == 0 ? "" : lines[0];
    }
    StringBuilder sb = new StringBuilder(lines[0]);
    for (int i = 1; i < count; i++) {
      sb.append('\n');
      sb.append(lines[i]);
    }
    return sb.toString();
  }

  public String2d vBar(String2d left, String2d right) {
    int top = Math.max(left.baseline, right.baseline);
    int leftBottom = left.height() - left.baseline;
    int rightBottom = right.height() - right.baseline;
    int bottom = Math.max(leftBottom, rightBottom);
    int height = top + bottom;

    return concat(left, vline(height, top, '\u23aa'), right);

  }

  public static class Builder {
    ArrayList<Object> parts = new ArrayList<>();

    public void append(Object o) {
      if (o instanceof String2d.Builder) {
        parts.add(((String2d.Builder) o).build());
      } else if (o instanceof String2d) {
        parts.add(o);
      } else {
        parts.add(String.valueOf(o));
      }
    }

    public String2d build() {
      return concat(parts.toArray());
    }

    public boolean isEmpty() {
      return parts.size() == 0;
    }

    public int length() {
      int l = 0;
      for (Object o: parts) {
        if (o instanceof String2d) {
          l += ((String2d) o).width();
        } else {
          l += ((String) o).length();
        }
      }
      return l;
    }

    public int size() {
      return parts.size();
    }
  }
}
