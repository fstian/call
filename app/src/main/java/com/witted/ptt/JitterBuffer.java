package com.witted.ptt;

public class JitterBuffer {
	private native int InitJb();
	private native int OpenJb(int codec,int ptime,int dataRate);
	private native int CloseJb(int jbline);
	private native int ResetJb(int jbline);
	private native int AddPackage(int jbline,byte[] src,int size);
	private native int GetPackage(int jbline,byte[] dst,int size);
	private native int GetStatus(int jbline,byte[] dst,int size);
	private native int GetJbVer();
	private native int DeInitJb();

	public int initJb(){
		synchronized (this)
		{
			return InitJb();
		}
	}

	public int openJb(int codec,int ptime,int dataRate){
		synchronized (this)
		{
			return OpenJb(codec,ptime,dataRate);
		}
	}

	public int resetJb(int jbline){
		synchronized (this)
		{
			return ResetJb(jbline);
		}
	}

	public int closeJb(int jbline){
		synchronized (this)
		{
			return CloseJb(jbline);
		}
	}

	public int addPackage(int jbline,byte[] src,int size){
		synchronized (this)
		{
			return AddPackage(jbline,src,size);
		}
	}

	public  int getPackage(int jbline,byte[] dst,int size){
		synchronized (this)
		{
			return GetPackage(jbline,dst,size);
		}
	}
	public int getStatus(int jbline,byte[] dst,int size){
		synchronized (this)
		{
			return GetStatus(jbline,dst,size);
		}

	}
	public int getJbVer(){
		synchronized (this)
		{
			return GetJbVer();
		}
	}
	public int deInitJb(){
		synchronized (this)
		{
			return DeInitJb();
		}

	}

}
