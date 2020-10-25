package software.amazon.kms.grant;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.util.StringUtils;
import java.util.Optional;
import software.amazon.awssdk.services.kms.KmsClient;
import software.amazon.awssdk.services.kms.model.DependencyTimeoutException;
import software.amazon.awssdk.services.kms.model.GrantListEntry;
import software.amazon.awssdk.services.kms.model.InvalidArnException;
import software.amazon.awssdk.services.kms.model.InvalidGrantIdException;
import software.amazon.awssdk.services.kms.model.InvalidMarkerException;
import software.amazon.awssdk.services.kms.model.KmsInternalException;
import software.amazon.awssdk.services.kms.model.KmsInvalidStateException;
import software.amazon.awssdk.services.kms.model.ListGrantsRequest;
import software.amazon.awssdk.services.kms.model.ListGrantsResponse;
import software.amazon.awssdk.services.kms.model.NotFoundException;
import software.amazon.cloudformation.exceptions.CfnGeneralServiceException;
import software.amazon.cloudformation.exceptions.CfnInternalFailureException;
import software.amazon.cloudformation.exceptions.CfnInvalidRequestException;
import software.amazon.cloudformation.exceptions.CfnNotFoundException;
import software.amazon.cloudformation.exceptions.CfnServiceInternalErrorException;
import software.amazon.cloudformation.exceptions.CfnThrottlingException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

public class ReadHandler extends BaseHandlerStd {
    private static final String OPERATION = "ListGrants";

    @Override
    protected ProgressEvent<ResourceModel, CallbackContext> handleRequest(
        final AmazonWebServicesClientProxy proxy,
        final ResourceHandlerRequest<ResourceModel> request,
        final CallbackContext callbackContext,
        final ProxyClient<KmsClient> proxyClient,
        final Logger logger) {

        final ResourceModel model = request.getDesiredResourceState();

        // Currently, it is only possible to read a grant by listing all grants for that key
        String marker = null;
        do {
            final ListGrantsResponse listGrantsResponse = listGrants(Translator
                .translateToListGrantsRequest(model, marker), proxyClient);

            final Optional<GrantListEntry> grant = listGrantsResponse.grants().stream()
                .filter(g -> g.grantId().equals(model.getGrantId())).findFirst();
            if (grant.isPresent()) {
                return ProgressEvent.defaultSuccessHandler(Translator
                    .translateToResourceModel(grant.get()));
            }
            marker = listGrantsResponse.nextMarker();
        } while (!StringUtils.isNullOrEmpty(marker));

        throw new CfnNotFoundException(ResourceModel.TYPE_NAME, model.getGrantId());
    }

    private ListGrantsResponse listGrants(final ListGrantsRequest listGrantsRequest,
                                         final ProxyClient<KmsClient> proxyClient) {
        try {
            return proxyClient.injectCredentialsAndInvokeV2(listGrantsRequest,
                proxyClient.client()::listGrants);
        } catch (final NotFoundException e) {
            throw new CfnNotFoundException(e);
        } catch (final KmsInternalException | DependencyTimeoutException e) {
            throw new CfnServiceInternalErrorException(OPERATION, e);
        } catch (final InvalidArnException | KmsInvalidStateException e) {
            throw new CfnInvalidRequestException(e.getMessage());
        } catch (final InvalidMarkerException e) {
            throw new CfnInternalFailureException(e);
        } catch (final AmazonServiceException exception) {
            if (THROTTLING_ERROR_CODE.equals(exception.getErrorCode())) {
                throw new CfnThrottlingException(OPERATION, exception);
            }

            throw new CfnGeneralServiceException(OPERATION, exception);
        }
    }
}
