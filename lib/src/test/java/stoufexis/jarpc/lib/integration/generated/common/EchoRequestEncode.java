package stoufexis.jarpc.lib.integration.generated.common;

import stoufexis.jarpc.lib.common.*;

public interface EchoRequestEncode {

  void setBool(boolean bool);

  void setBite(byte bite);

  void setSort(short sort);

  void setEent(int eent);

  void setLog(long log);

  void setFlowt(float flowt);

  void setTwice(double twice);

  void setSBytes(Bytes sBytes);

  void setMBytes(Bytes mBytes);

  void setLBytes(Bytes lBytes);


  default void set(EchoRequestDecode decode) {

    setBool(decode.bool());

    setBite(decode.bite());

    setSort(decode.sort());

    setEent(decode.eent());

    setLog(decode.log());

    setFlowt(decode.flowt());

    setTwice(decode.twice());

    setSBytes(decode.sBytes());

    setMBytes(decode.mBytes());

    setLBytes(decode.lBytes());


  }
}

