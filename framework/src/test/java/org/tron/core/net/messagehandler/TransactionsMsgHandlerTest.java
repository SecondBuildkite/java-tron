package org.tron.core.net.messagehandler;

import com.google.protobuf.Any;
import com.google.protobuf.ByteString;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;
import org.joda.time.DateTime;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;
import org.tron.common.BaseTest;
import org.tron.common.utils.ByteArray;
import org.tron.core.Constant;
import org.tron.core.config.args.Args;
import org.tron.core.net.TronNetDelegate;
import org.tron.core.net.message.adv.TransactionMessage;
import org.tron.core.net.message.adv.TransactionsMessage;
import org.tron.core.net.peer.Item;
import org.tron.core.net.peer.PeerConnection;
import org.tron.core.net.service.adv.AdvService;
import org.tron.protos.Protocol;
import org.tron.protos.contract.BalanceContract;

@Slf4j
public class TransactionsMsgHandlerTest extends BaseTest {
  @BeforeClass
  public static void init() {
    dbPath = "output_trxmsghandler_test";
    Args.setParam(new String[]{"--output-directory", dbPath, "--debug"},
        Constant.TEST_CONF);

  }

  @Test
  public void testProcessMessage() {
    TransactionsMsgHandler transactionsMsgHandler = new TransactionsMsgHandler();
    try {
      Assert.assertFalse(transactionsMsgHandler.isBusy());

      transactionsMsgHandler.init();

      PeerConnection peer = Mockito.mock(PeerConnection.class);
      TronNetDelegate tronNetDelegate = Mockito.mock(TronNetDelegate.class);
      AdvService advService = Mockito.mock(AdvService.class);

      Field field = TransactionsMsgHandler.class.getDeclaredField("tronNetDelegate");
      field.setAccessible(true);
      field.set(transactionsMsgHandler, tronNetDelegate);

      BalanceContract.TransferContract transferContract = BalanceContract.TransferContract
          .newBuilder()
          .setAmount(10)
          .setOwnerAddress(ByteString.copyFrom(ByteArray.fromHexString("121212a9cf")))
          .setToAddress(ByteString.copyFrom(ByteArray.fromHexString("232323a9cf"))).build();

      long transactionTimestamp = DateTime.now().minusDays(4).getMillis();
      Protocol.Transaction trx = Protocol.Transaction.newBuilder().setRawData(
          Protocol.Transaction.raw.newBuilder().setTimestamp(transactionTimestamp)
          .setRefBlockNum(1)
          .addContract(
              Protocol.Transaction.Contract.newBuilder()
                  .setType(Protocol.Transaction.Contract.ContractType.TransferContract)
                  .setParameter(Any.pack(transferContract)).build()).build())
          .build();
      Map<Item, Long> advInvRequest = new ConcurrentHashMap<>();
      Item item = new Item(new TransactionMessage(trx).getMessageId(),
          Protocol.Inventory.InventoryType.TRX);
      advInvRequest.put(item, 0L);
      Mockito.when(peer.getAdvInvRequest()).thenReturn(advInvRequest);

      List<Protocol.Transaction> transactionList = new ArrayList<>();
      transactionList.add(trx);
      transactionsMsgHandler.processMessage(peer, new TransactionsMessage(transactionList));
      Assert.assertNull(advInvRequest.get(item));
      //Thread.sleep(10);
    } catch (Exception e) {
      logger.error("error", e);
    } finally {
      transactionsMsgHandler.close();
    }
  }
}
