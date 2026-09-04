package stoufexis.jarpc.lib.integration.generated.common;

import stoufexis.jarpc.lib.common.*;

public interface EchoResponseEncode {

  void setLBytes(Bytes lBytes);

  void setMBytes(Bytes mBytes);

  void setSBytes(Bytes sBytes);

  void setTwice(double twice);

  void setFlowt(float flowt);

  void setLog(long log);

  void setEent(int eent);

  void setSort(short sort);

  void setBite(byte bite);

  void setBool(boolean bool);



  default void set(EchoResponseDecode decode) {

    setLBytes(decode.lBytes());

    setMBytes(decode.mBytes());

    setSBytes(decode.sBytes());

    setTwice(decode.twice());

    setFlowt(decode.flowt());

    setLog(decode.log());

    setEent(decode.eent());

    setSort(decode.sort());

    setBite(decode.bite());

    setBool(decode.bool());


  }
}

