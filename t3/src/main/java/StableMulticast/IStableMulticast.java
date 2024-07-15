package StableMulticast;

public interface IStableMulticast {
    void deliver(String msg);

//    TODO: método não presente na especificação do trabalho. Portanto, não estará presente no cliente de teste e o middleware não pode depender de sua existência. Remover todas referências
    String getClientName();
}
