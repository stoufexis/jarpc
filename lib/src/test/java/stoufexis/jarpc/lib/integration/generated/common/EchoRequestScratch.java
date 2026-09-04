package stoufexis.jarpc.lib.integration.generated.common;

import stoufexis.jarpc.lib.common.*;
import stoufexis.jarpc.lib.integration.generated.client.EchoConcurrentClient;

public final class EchoRequestScratch implements EchoRequestDecode, EchoRequestEncode {

  private boolean bool = false;

  private byte bite = 0;

  private short sort = 0;

  private int eent = 0;

  private long log = 0;

  private float flowt = 0;

  private double twice = 0;

  private Bytes sBytes = new Bytes(16);

  private Bytes mBytes = new Bytes(32);

  private Bytes lBytes = new Bytes(64);


  private EchoConcurrentClient.EchoResponseHandler handler;

  public static void copy(EchoRequestScratch scratch1, EchoRequestScratch scratch2) {
    setter(scratch1, scratch2, scratch2.getHandler());
  }

  public static void setter(
      EchoRequestScratch scratch,
      EchoRequestDecode decode,
      EchoConcurrentClient.EchoResponseHandler handler) {
    scratch.set(decode);
    scratch.setHandler(handler);
  }

  public EchoConcurrentClient.EchoResponseHandler getHandler() {
    return handler;
  }


  @Override
  public boolean bool() {
    return bool;
  }

  @Override
  public void setBool(boolean bool) {
    this.bool = bool;
  }

  @Override
  public byte bite() {
    return bite;
  }

  @Override
  public void setBite(byte bite) {
    this.bite = bite;
  }

  @Override
  public short sort() {
    return sort;
  }

  @Override
  public void setSort(short sort) {
    this.sort = sort;
  }

  @Override
  public int eent() {
    return eent;
  }

  @Override
  public void setEent(int eent) {
    this.eent = eent;
  }

  @Override
  public long log() {
    return log;
  }

  @Override
  public void setLog(long log) {
    this.log = log;
  }

  @Override
  public float flowt() {
    return flowt;
  }

  @Override
  public void setFlowt(float flowt) {
    this.flowt = flowt;
  }

  @Override
  public double twice() {
    return twice;
  }

  @Override
  public void setTwice(double twice) {
    this.twice = twice;
  }

  @Override
  public Bytes sBytes() {
    return sBytes;
  }

  @Override
  public void setSBytes(Bytes sBytes) {
    this.sBytes.copy(sBytes);
  }

  @Override
  public Bytes mBytes() {
    return mBytes;
  }

  @Override
  public void setMBytes(Bytes mBytes) {
    this.mBytes.copy(mBytes);
  }

  @Override
  public Bytes lBytes() {
    return lBytes;
  }

  @Override
  public void setLBytes(Bytes lBytes) {
    this.lBytes.copy(lBytes);
  }


  public void setHandler(EchoConcurrentClient.EchoResponseHandler handler) {
    this.handler = handler;
  }
}

