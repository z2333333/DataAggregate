import DataAggregate.PurchaseDetailResp;
import com.vd.canary.obmp.OrderApplication;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * @author zx
 * @date 2020/12/28 16:06
 */
@RunWith(SpringRunner.class)
@TestPropertySource(properties = {"spring.application.name=obmp-order-service","spring.profiles.active=dev_c"})
@SpringBootTest(classes = OrderApplication.class)
//@ActiveProfiles("dev_c")
public class DataAggregateTest {

    /**
     * 正常
     * @return
     */
    public PurchaseDetailResp purchaseOrderDetailTest(){
        return new PurchaseDetailResp();
    }

}


