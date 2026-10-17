package android.media;
public final class AudioAttributes {
    public static final int USAGE_MEDIA = 1, CONTENT_TYPE_SPEECH = 1;
    public int usage, contentType;
    public static final class Builder {
        private final AudioAttributes value = new AudioAttributes();
        public Builder setUsage(int usage) { value.usage = usage; return this; }
        public Builder setContentType(int type) { value.contentType = type; return this; }
        public AudioAttributes build() { return value; }
    }
}
