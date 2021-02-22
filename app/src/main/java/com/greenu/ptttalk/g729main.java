package com.greenu.ptttalk;

public class g729main {
    public native String GetG729Ver(int i);
    public native int G729ToLinear(byte[] src,int srcsize,short[] dst);
    public native int LinearToG729(short[] src,int srcsize,byte[] dst);
    public native int G729Init(int i);
    public native int G929DeInit(int i);
}
