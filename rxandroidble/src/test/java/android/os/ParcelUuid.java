package android.os;
import java.util.UUID;
public final class ParcelUuid implements Parcelable {
    private final UUID mUuid;
    public ParcelUuid(UUID uuid) {
        mUuid = uuid;
    }
    public static ParcelUuid fromString(String uuid) {
        return new ParcelUuid(UUID.fromString(uuid));
    }
    public UUID getUuid() {
        return mUuid;
    }
    @Override
    public String toString() {
        return mUuid.toString();
    }
    @Override
    public int hashCode() {
        return mUuid.hashCode();
    }
    @Override
    public boolean equals(Object object) {
        if (object == null) {
            return false;
        }
        if (this == object) {
            return true;
        }
        if (!(object instanceof ParcelUuid)) {
            return false;
        }
        ParcelUuid that = (ParcelUuid) object;
        return (this.mUuid.equals(that.mUuid));
    }
    public static final Creator<ParcelUuid> CREATOR =
            new Creator<ParcelUuid>() {
                public ParcelUuid createFromParcel(Parcel source) {
                    long mostSigBits = source.readLong();
                    long leastSigBits = source.readLong();
                    UUID uuid = new UUID(mostSigBits, leastSigBits);
                    return new ParcelUuid(uuid);
                }
                public ParcelUuid[] newArray(int size) {
                    return new ParcelUuid[size];
                }
            };
    public int describeContents() {
        return 0;
    }
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(mUuid.getMostSignificantBits());
        dest.writeLong(mUuid.getLeastSignificantBits());
    }
}
