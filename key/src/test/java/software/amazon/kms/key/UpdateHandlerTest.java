package software.amazon.kms.key;

import java.time.Duration;
import org.junit.jupiter.api.AfterEach;
import software.amazon.awssdk.services.kms.KmsClient;
import software.amazon.awssdk.services.kms.model.DescribeKeyRequest;
import software.amazon.awssdk.services.kms.model.DescribeKeyResponse;
import software.amazon.awssdk.services.kms.model.DisableKeyResponse;
import software.amazon.awssdk.services.kms.model.DisableKeyRequest;
import software.amazon.awssdk.services.kms.model.DisableKeyRotationResponse;
import software.amazon.awssdk.services.kms.model.DisableKeyRotationRequest;
import software.amazon.awssdk.services.kms.model.EnableKeyResponse;
import software.amazon.awssdk.services.kms.model.EnableKeyRequest;
import software.amazon.awssdk.services.kms.model.GetKeyPolicyRequest;
import software.amazon.awssdk.services.kms.model.GetKeyPolicyResponse;
import software.amazon.awssdk.services.kms.model.GetKeyRotationStatusResponse;
import software.amazon.awssdk.services.kms.model.PutKeyPolicyResponse;
import software.amazon.awssdk.services.kms.model.PutKeyPolicyRequest;
import software.amazon.awssdk.services.kms.model.ListResourceTagsResponse;
import software.amazon.awssdk.services.kms.model.ListResourceTagsRequest;
import software.amazon.awssdk.services.kms.model.GetKeyRotationStatusRequest;
import software.amazon.awssdk.services.kms.model.KeyMetadata;
import software.amazon.awssdk.services.kms.model.TagResourceRequest;
import software.amazon.awssdk.services.kms.model.TagResourceResponse;
import software.amazon.awssdk.services.kms.model.UntagResourceRequest;
import software.amazon.awssdk.services.kms.model.UntagResourceResponse;
import software.amazon.awssdk.services.kms.model.UpdateKeyDescriptionResponse;
import software.amazon.awssdk.services.kms.model.UpdateKeyDescriptionRequest;
import software.amazon.cloudformation.exceptions.CfnInvalidRequestException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class UpdateHandlerTest extends AbstractTestBase{

    @Mock
    private AmazonWebServicesClientProxy proxy;

    @Mock
    private ProxyClient<KmsClient> proxyKmsClient;

    @Mock
    KmsClient kms;

    private UpdateHandler handler;

    @BeforeEach
    public void setup() {
        handler = new UpdateHandler();
        kms = mock(KmsClient.class);
        proxy = new AmazonWebServicesClientProxy(logger, MOCK_CREDENTIALS, () -> Duration.ofSeconds(600).toMillis());
        proxyKmsClient = MOCK_PROXY(proxy, kms);
    }

    @AfterEach
    public void post_execute() {
        verify(kms, atLeastOnce()).serviceName();
        verifyNoMoreInteractions(proxyKmsClient.client());
    }


    @Test
    public void handleRequest_SimpleSuccess() {

        final KeyMetadata keyMetadata = KeyMetadata.builder()
            .keyId("sampleId")
            .arn("sampleArn")
            .enabled(true)
            .build();

        final DescribeKeyResponse describeKeyResponse = DescribeKeyResponse.builder().keyMetadata(keyMetadata).build();
        when(proxyKmsClient.client().describeKey(any(DescribeKeyRequest.class))).thenReturn(describeKeyResponse);

        final GetKeyRotationStatusResponse getKeyRotationStatusResponse = GetKeyRotationStatusResponse.builder().keyRotationEnabled(true).build();
        when(proxyKmsClient.client().getKeyRotationStatus(any(GetKeyRotationStatusRequest.class))).thenReturn(getKeyRotationStatusResponse);

        final DisableKeyRotationResponse disableKeyRotationResponse = DisableKeyRotationResponse.builder().build();
        when(proxyKmsClient.client().disableKeyRotation(any(DisableKeyRotationRequest.class))).thenReturn(disableKeyRotationResponse);

        final DisableKeyResponse disableKeyResponse = DisableKeyResponse.builder().build();
        when(proxyKmsClient.client().disableKey(any(DisableKeyRequest.class))).thenReturn(disableKeyResponse);

        final UpdateKeyDescriptionResponse updateKeyDescriptionResponse = UpdateKeyDescriptionResponse.builder().build();
        when(proxyKmsClient.client().updateKeyDescription(any(UpdateKeyDescriptionRequest.class))).thenReturn(updateKeyDescriptionResponse);

        final PutKeyPolicyResponse putKeyPolicyResponse = PutKeyPolicyResponse.builder().build();
        when(proxyKmsClient.client().putKeyPolicy(any(PutKeyPolicyRequest.class))).thenReturn(putKeyPolicyResponse);

        final ListResourceTagsResponse listTagsForResourceResponse = ListResourceTagsResponse.builder().build();
        when(proxyKmsClient.client().listResourceTags(any(ListResourceTagsRequest.class))).thenReturn(listTagsForResourceResponse);

        final UntagResourceResponse untagResourceResponse = UntagResourceResponse.builder().build();
        when(proxyKmsClient.client().untagResource(any(UntagResourceRequest.class))).thenReturn(untagResourceResponse);

        final TagResourceResponse tagResourceResponse = TagResourceResponse.builder().build();
        when(proxyKmsClient.client().tagResource(any(TagResourceRequest.class))).thenReturn(tagResourceResponse);

        final GetKeyPolicyResponse getKeyPolicyResponse = GetKeyPolicyResponse.builder().policy("{\"foo\": \"bar\"}").build();
        when(proxyKmsClient.client().getKeyPolicy(any(GetKeyPolicyRequest.class))).thenReturn(getKeyPolicyResponse);


        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
            .desiredResourceState(
                ResourceModel.builder()
                    .enabled(false)
                    .enableKeyRotation(false)
                    .build())
            .build();
        final CallbackContext callbackContext = new CallbackContext();
        callbackContext.setPropagated(true);
        final ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(proxy, request, callbackContext, proxyKmsClient, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackContext()).isNotNull();
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getCallbackContext().propagated).isEqualTo(true);
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();

        verify(proxyKmsClient.client(), times(2)).describeKey(any(DescribeKeyRequest.class));
        verify(proxyKmsClient.client(), times(2)).getKeyRotationStatus(any(GetKeyRotationStatusRequest.class));
        verify(proxyKmsClient.client()).disableKey(any(DisableKeyRequest.class));
        verify(proxyKmsClient.client()).updateKeyDescription(any(UpdateKeyDescriptionRequest.class));
        verify(proxyKmsClient.client()).putKeyPolicy(any(PutKeyPolicyRequest.class));
        verify(proxyKmsClient.client(), times(2)).listResourceTags(any(ListResourceTagsRequest.class));
        verify(proxyKmsClient.client()).untagResource(any(UntagResourceRequest.class));
        verify(proxyKmsClient.client()).tagResource(any(TagResourceRequest.class));
        verify(proxyKmsClient.client()).getKeyPolicy(any(GetKeyPolicyRequest.class));
    }

    @Test
    public void handleRequest_InvalidRequest() {

        final KeyMetadata keyMetadata = KeyMetadata.builder()
            .keyId("sampleId")
            .arn("sampleArn")
            .enabled(false)
            .build();

        final DescribeKeyResponse describeKeyResponse = DescribeKeyResponse.builder().keyMetadata(keyMetadata).build();
        when(proxyKmsClient.client().describeKey(any(DescribeKeyRequest.class))).thenReturn(describeKeyResponse);

        final GetKeyRotationStatusResponse getKeyRotationStatusResponse = GetKeyRotationStatusResponse.builder().keyRotationEnabled(true).build();
        when(proxyKmsClient.client().getKeyRotationStatus(any(GetKeyRotationStatusRequest.class))).thenReturn(getKeyRotationStatusResponse);

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
            .desiredResourceState(
                ResourceModel.builder()
                    .enabled(false)
                    .enableKeyRotation(false)
                    .description("sampleDescription")
                    .keyUsage("ENCRYPT_DECRYPT")
                    .build())
            .build();

        try {
            handler.handleRequest(proxy, request, new CallbackContext(), proxyKmsClient, logger);
        } catch (CfnInvalidRequestException e) {
            assertThat(e.getMessage()).isEqualTo("Invalid request provided: You cannot modify the EnableKeyRotation property when the Enabled property is false. Set Enabled to true to modify the EnableKeyRotation property.");
        }

        verify(proxyKmsClient.client()).describeKey(any(DescribeKeyRequest.class));
        verify(proxyKmsClient.client()).getKeyRotationStatus(any(GetKeyRotationStatusRequest.class));
    }

    @Test
    public void handleRequest_KeyStatusRotationUpdateV1() {
        final KeyMetadata keyMetadata = KeyMetadata.builder()
            .keyId("sampleId")
            .arn("sampleArn")
            .enabled(false)
            .build();

        final DescribeKeyResponse describeKeyResponse = DescribeKeyResponse.builder().keyMetadata(keyMetadata).build();
        when(proxyKmsClient.client().describeKey(any(DescribeKeyRequest.class))).thenReturn(describeKeyResponse);

        final GetKeyRotationStatusResponse getKeyRotationStatusResponse = GetKeyRotationStatusResponse.builder().keyRotationEnabled(true).build();
        when(proxyKmsClient.client().getKeyRotationStatus(any(GetKeyRotationStatusRequest.class))).thenReturn(getKeyRotationStatusResponse);

        final EnableKeyResponse enableKeyResponse = EnableKeyResponse.builder().build();
        when(proxyKmsClient.client().enableKey(any(EnableKeyRequest.class))).thenReturn(enableKeyResponse);



        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
            .desiredResourceState(
                ResourceModel.builder()
                    .enabled(true)
                    .enableKeyRotation(false)
                    .build())
            .build();

        final ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(proxy, request, new CallbackContext(), proxyKmsClient, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.IN_PROGRESS);
        assertThat(response.getCallbackContext()).isNotNull();
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(60);
        assertThat(response.getCallbackContext().keyEnabled).isEqualTo(true);
        assertThat(response.getCallbackContext().propagated).isEqualTo(false);
        assertThat(response.getResourceModel()).isEqualTo(request.getDesiredResourceState());
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();

        verify(proxyKmsClient.client()).describeKey(any(DescribeKeyRequest.class));
        verify(proxyKmsClient.client()).getKeyRotationStatus(any(GetKeyRotationStatusRequest.class));
        verify(proxyKmsClient.client()).enableKey(any(EnableKeyRequest.class));
    }

    @Test
    public void handleRequest_KeyStatusRotationUpdateV2() {
        final KeyMetadata keyMetadata = KeyMetadata.builder()
            .keyId("sampleId")
            .arn("sampleArn")
            .enabled(true)
            .build();

        final DescribeKeyResponse describeKeyResponse = DescribeKeyResponse.builder().keyMetadata(keyMetadata).build();
        when(proxyKmsClient.client().describeKey(any(DescribeKeyRequest.class))).thenReturn(describeKeyResponse);

        final GetKeyRotationStatusResponse getKeyRotationStatusResponse = GetKeyRotationStatusResponse.builder().keyRotationEnabled(true).build();
        when(proxyKmsClient.client().getKeyRotationStatus(any(GetKeyRotationStatusRequest.class))).thenReturn(getKeyRotationStatusResponse);

        final DisableKeyRotationResponse disableKeyRotationResponse = DisableKeyRotationResponse.builder().build();
        when(proxyKmsClient.client().disableKeyRotation(any(DisableKeyRotationRequest.class))).thenReturn(disableKeyRotationResponse);

        final DisableKeyResponse disableKeyResponse = DisableKeyResponse.builder().build();
        when(proxyKmsClient.client().disableKey(any(DisableKeyRequest.class))).thenReturn(disableKeyResponse);

        final UpdateKeyDescriptionResponse updateKeyDescriptionResponse = UpdateKeyDescriptionResponse.builder().build();
        when(proxyKmsClient.client().updateKeyDescription(any(UpdateKeyDescriptionRequest.class))).thenReturn(updateKeyDescriptionResponse);

        final PutKeyPolicyResponse putKeyPolicyResponse = PutKeyPolicyResponse.builder().build();
        when(proxyKmsClient.client().putKeyPolicy(any(PutKeyPolicyRequest.class))).thenReturn(putKeyPolicyResponse);

        final ListResourceTagsResponse listTagsForResourceResponse = ListResourceTagsResponse.builder().build();
        when(proxyKmsClient.client().listResourceTags(any(ListResourceTagsRequest.class))).thenReturn(listTagsForResourceResponse);

        final UntagResourceResponse untagResourceResponse = UntagResourceResponse.builder().build();
        when(proxyKmsClient.client().untagResource(any(UntagResourceRequest.class))).thenReturn(untagResourceResponse);

        final TagResourceResponse tagResourceResponse = TagResourceResponse.builder().build();
        when(proxyKmsClient.client().tagResource(any(TagResourceRequest.class))).thenReturn(tagResourceResponse);

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
            .desiredResourceState(
                ResourceModel.builder()
                    .enabled(false)
                    .enableKeyRotation(false)
                    .build())
            .build();

        final ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(proxy, request, new CallbackContext(), proxyKmsClient, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.IN_PROGRESS);
        assertThat(response.getCallbackContext()).isNotNull();
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(60);
        assertThat(response.getCallbackContext().keyEnabled).isEqualTo(false);
        assertThat(response.getCallbackContext().propagated).isEqualTo(true);
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();

        verify(proxyKmsClient.client()).describeKey(any(DescribeKeyRequest.class));
        verify(proxyKmsClient.client()).getKeyRotationStatus(any(GetKeyRotationStatusRequest.class));
        verify(proxyKmsClient.client()).disableKeyRotation(any(DisableKeyRotationRequest.class));

        verify(proxyKmsClient.client()).disableKey(any(DisableKeyRequest.class));
        verify(proxyKmsClient.client()).updateKeyDescription(any(UpdateKeyDescriptionRequest.class));
        verify(proxyKmsClient.client()).putKeyPolicy(any(PutKeyPolicyRequest.class));
        verify(proxyKmsClient.client()).listResourceTags(any(ListResourceTagsRequest.class));
        verify(proxyKmsClient.client()).untagResource(any(UntagResourceRequest.class));
        verify(proxyKmsClient.client()).tagResource(any(TagResourceRequest.class));
    }

    @Test
    public void handleRequest_KeyStatusRotationUpdateV3() {
        final KeyMetadata keyMetadata = KeyMetadata.builder()
            .keyId("sampleId")
            .arn("sampleArn")
            .build();

        final DescribeKeyResponse describeKeyResponse = DescribeKeyResponse.builder().keyMetadata(keyMetadata).build();
        when(proxyKmsClient.client().describeKey(any(DescribeKeyRequest.class))).thenReturn(describeKeyResponse);

        final UpdateKeyDescriptionResponse updateKeyDescriptionResponse = UpdateKeyDescriptionResponse.builder().build();
        when(proxyKmsClient.client().updateKeyDescription(any(UpdateKeyDescriptionRequest.class))).thenReturn(updateKeyDescriptionResponse);

        final PutKeyPolicyResponse putKeyPolicyResponse = PutKeyPolicyResponse.builder().build();
        when(proxyKmsClient.client().putKeyPolicy(any(PutKeyPolicyRequest.class))).thenReturn(putKeyPolicyResponse);

        final ListResourceTagsResponse listTagsForResourceResponse = ListResourceTagsResponse.builder().build();
        when(proxyKmsClient.client().listResourceTags(any(ListResourceTagsRequest.class))).thenReturn(listTagsForResourceResponse);

        final UntagResourceResponse untagResourceResponse = UntagResourceResponse.builder().build();
        when(proxyKmsClient.client().untagResource(any(UntagResourceRequest.class))).thenReturn(untagResourceResponse);

        final TagResourceResponse tagResourceResponse = TagResourceResponse.builder().build();
        when(proxyKmsClient.client().tagResource(any(TagResourceRequest.class))).thenReturn(tagResourceResponse);

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
            .desiredResourceState(
                ResourceModel.builder()
                    .enabled(false)
                    .enableKeyRotation(false)
                    .build())
            .build();

        final CallbackContext callbackContext = new CallbackContext();
        callbackContext.setKeyStatusRotationUpdated(true);

        final ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(proxy, request, callbackContext, proxyKmsClient, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.IN_PROGRESS);
        assertThat(response.getCallbackContext()).isNotNull();
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(60);
        assertThat(response.getCallbackContext().keyEnabled).isEqualTo(false);
        assertThat(response.getCallbackContext().propagated).isEqualTo(true);
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();

        verify(proxyKmsClient.client()).describeKey(any(DescribeKeyRequest.class));
        verify(proxyKmsClient.client()).updateKeyDescription(any(UpdateKeyDescriptionRequest.class));
        verify(proxyKmsClient.client()).putKeyPolicy(any(PutKeyPolicyRequest.class));
        verify(proxyKmsClient.client()).listResourceTags(any(ListResourceTagsRequest.class));
        verify(proxyKmsClient.client()).untagResource(any(UntagResourceRequest.class));
        verify(proxyKmsClient.client()).tagResource(any(TagResourceRequest.class));
    }

    @Test
    public void handleRequest_KeyStatusRotationUpdateV4() {
        final KeyMetadata keyMetadata = KeyMetadata.builder()
            .keyId("sampleId")
            .arn("sampleArn")
            .enabled(true)
            .build();

        final DescribeKeyResponse describeKeyResponse = DescribeKeyResponse.builder().keyMetadata(keyMetadata).build();
        when(proxyKmsClient.client().describeKey(any(DescribeKeyRequest.class))).thenReturn(describeKeyResponse);

        final GetKeyRotationStatusResponse getKeyRotationStatusResponse = GetKeyRotationStatusResponse.builder().keyRotationEnabled(false).build();
        when(proxyKmsClient.client().getKeyRotationStatus(any(GetKeyRotationStatusRequest.class))).thenReturn(getKeyRotationStatusResponse);

        final UpdateKeyDescriptionResponse updateKeyDescriptionResponse = UpdateKeyDescriptionResponse.builder().build();
        when(proxyKmsClient.client().updateKeyDescription(any(UpdateKeyDescriptionRequest.class))).thenReturn(updateKeyDescriptionResponse);

        final PutKeyPolicyResponse putKeyPolicyResponse = PutKeyPolicyResponse.builder().build();
        when(proxyKmsClient.client().putKeyPolicy(any(PutKeyPolicyRequest.class))).thenReturn(putKeyPolicyResponse);

        final ListResourceTagsResponse listTagsForResourceResponse = ListResourceTagsResponse.builder().build();
        when(proxyKmsClient.client().listResourceTags(any(ListResourceTagsRequest.class))).thenReturn(listTagsForResourceResponse);

        final UntagResourceResponse untagResourceResponse = UntagResourceResponse.builder().build();
        when(proxyKmsClient.client().untagResource(any(UntagResourceRequest.class))).thenReturn(untagResourceResponse);

        final TagResourceResponse tagResourceResponse = TagResourceResponse.builder().build();
        when(proxyKmsClient.client().tagResource(any(TagResourceRequest.class))).thenReturn(tagResourceResponse);


        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
            .desiredResourceState(
                ResourceModel.builder()
                    .enabled(true)
                    .enableKeyRotation(false)
                    .build())
            .build();

        final ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(proxy, request, new CallbackContext(), proxyKmsClient, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.IN_PROGRESS);
        assertThat(response.getCallbackContext()).isNotNull();
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(60);
        assertThat(response.getCallbackContext().keyEnabled).isEqualTo(false);
        assertThat(response.getCallbackContext().propagated).isEqualTo(true);
        assertThat(response.getResourceModel()).isEqualTo(request.getDesiredResourceState());
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();

        verify(proxyKmsClient.client()).describeKey(any(DescribeKeyRequest.class));
        verify(proxyKmsClient.client()).getKeyRotationStatus(any(GetKeyRotationStatusRequest.class));
        verify(proxyKmsClient.client()).updateKeyDescription(any(UpdateKeyDescriptionRequest.class));
        verify(proxyKmsClient.client()).putKeyPolicy(any(PutKeyPolicyRequest.class));
        verify(proxyKmsClient.client()).listResourceTags(any(ListResourceTagsRequest.class));
        verify(proxyKmsClient.client()).untagResource(any(UntagResourceRequest.class));
        verify(proxyKmsClient.client()).tagResource(any(TagResourceRequest.class));
    }
}