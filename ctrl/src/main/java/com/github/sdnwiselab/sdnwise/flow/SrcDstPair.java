package com.github.sdnwiselab.sdnwise.flow;

/**
 * Created by cemturker on 21.11.2018.
 */
public class SrcDstPair {
    private String src;
    private String dst;
    private int networkId;

    public SrcDstPair(String src, String dst, int networkId) {
        this.src = src;
        this.dst = dst;
        this.networkId = networkId;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("SrcDstPair{");
        sb.append("src='").append(src).append('\'');
        sb.append(", dst='").append(dst).append('\'');
        sb.append(", networkId=").append(networkId);
        sb.append('}');
        return sb.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        SrcDstPair pair = (SrcDstPair) o;

        if (networkId != pair.networkId) return false;
        if (src != null ? !src.equals(pair.src) : pair.src != null) return false;
        return dst != null ? dst.equals(pair.dst) : pair.dst == null;
    }

    @Override
    public int hashCode() {
        int result = src != null ? src.hashCode() : 0;
        result = 31 * result + (dst != null ? dst.hashCode() : 0);
        result = 31 * result + networkId;
        return result;
    }

    public String getSrc() {
        return src;
    }

    public void setSrc(String src) {
        this.src = src;
    }

    public String getDst() {
        return dst;
    }

    public void setDst(String dst) {
        this.dst = dst;
    }

    public int getNetworkId() {
        return networkId;
    }

    public void setNetworkId(int networkId) {
        this.networkId = networkId;
    }
}
