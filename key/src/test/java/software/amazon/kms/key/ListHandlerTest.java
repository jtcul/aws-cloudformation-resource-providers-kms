package software.amazon.kms.key;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;


import java.time.Duration;
import java.util.Collections;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.kms.KmsClient;
import software.amazon.awssdk.services.kms.model.KeyListEntry;
import software.amazon.awssdk.services.kms.model.ListKeysRequest;
import software.amazon.awssdk.services.kms.model.ListKeysResponse;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

@ExtendWith(MockitoExtension.class)
public class ListHandlerTest extends AbstractTestBase {

    @Mock
    KmsClient kms;

    @Mock
    private KeyHelper keyHelper;

    private ListHandler handler;
    private AmazonWebServicesClientProxy proxy;
    private ProxyClient<KmsClient> proxyKmsClient;

    private static final String KEY_ID = "samplearn";
    private static final String NEXT_TOKEN = "4b90a7e4-b790-456b";

    @BeforeEach
    public void setup() {
        handler = new ListHandler(keyHelper);
        proxy = new AmazonWebServicesClientProxy(logger, MOCK_CREDENTIALS,
            () -> Duration.ofSeconds(600).toMillis());
        proxyKmsClient = MOCK_PROXY(proxy, kms);
    }

    @AfterEach
    public void post_execute() {
        verifyNoMoreInteractions(proxyKmsClient.client());
        verifyNoMoreInteractions(keyHelper);
    }

    @Test
    public void handleRequest_SimpleSuccess() {
        final ListKeysResponse listKeysResponse = ListKeysResponse.builder()
            .keys(Collections.singletonList(KeyListEntry.builder().keyId(KEY_ID).build()))
            .nextMarker(NEXT_TOKEN).build();
        when(keyHelper.listKeys(any(ListKeysRequest.class), eq(proxyKmsClient)))
            .thenReturn(listKeysResponse);

        final ResourceModel model = ResourceModel.builder().build();

        final ResourceModel expectedModel = ResourceModel.builder().keyId(KEY_ID).build();

        final ResourceHandlerRequest<ResourceModel> request =
            ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .build();

        final ProgressEvent<ResourceModel, CallbackContext> response =
            handler.handleRequest(proxy, request, new CallbackContext(), proxyKmsClient, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackContext()).isNull();
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isNull();
        assertThat(response.getResourceModels()).isNotNull();
        assertThat(response.getResourceModels()).containsExactly(expectedModel);
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
        assertThat(response.getNextToken()).isEqualTo(NEXT_TOKEN);

        verify(keyHelper).listKeys(any(ListKeysRequest.class), eq(proxyKmsClient));
    }
}
