public class NotificationReceiver implements UnicastServiceUserInterface {

    public void UPDataInd(short ucsapId, String notification) {
        System.out.println("Notificação Recebida de Identificador: " + ucsapId);
        System.out.println("Mensagem: " + notification);
        System.out.println("");
    }
}
