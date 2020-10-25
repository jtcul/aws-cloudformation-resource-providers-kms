# AWS::KMS::Grant

The AWS::KMS::Grant resource specifies an extra set of permissions that can be applied to a customer master key (CMK) in AWS Key Management Service (AWS KMS). With grants you can programmatically delegate the use of KMS customer master keys (CMKs) to other AWS principals. You can use them to allow access, but not deny it. Because grants can be very specific, and are easy to create and revoke, they are often used to provide temporary permissions or more granular permissions.

## Syntax

To declare this entity in your AWS CloudFormation template, use the following syntax:

### JSON

<pre>
{
    "Type" : "AWS::KMS::Grant",
    "Properties" : {
        "<a href="#keyid" title="KeyId">KeyId</a>" : <i>String</i>,
        "<a href="#granteeprincipal" title="GranteePrincipal">GranteePrincipal</a>" : <i>String</i>,
        "<a href="#retiringprincipal" title="RetiringPrincipal">RetiringPrincipal</a>" : <i>String</i>,
        "<a href="#operations" title="Operations">Operations</a>" : <i>[ String, ... ]</i>,
        "<a href="#constraints" title="Constraints">Constraints</a>" : <i><a href="constraints.md">Constraints</a></i>,
        "<a href="#granttokens" title="GrantTokens">GrantTokens</a>" : <i>[ String, ... ]</i>,
        "<a href="#name" title="Name">Name</a>" : <i>String</i>,
    }
}
</pre>

### YAML

<pre>
Type: AWS::KMS::Grant
Properties:
    <a href="#keyid" title="KeyId">KeyId</a>: <i>String</i>
    <a href="#granteeprincipal" title="GranteePrincipal">GranteePrincipal</a>: <i>String</i>
    <a href="#retiringprincipal" title="RetiringPrincipal">RetiringPrincipal</a>: <i>String</i>
    <a href="#operations" title="Operations">Operations</a>: <i>
      - String</i>
    <a href="#constraints" title="Constraints">Constraints</a>: <i><a href="constraints.md">Constraints</a></i>
    <a href="#granttokens" title="GrantTokens">GrantTokens</a>: <i>
      - String</i>
    <a href="#name" title="Name">Name</a>: <i>String</i>
</pre>

## Properties

#### KeyId

The unique identifier for the customer master key (CMK) that the grant applies to. Specify the key ID or the Amazon Resource Name (ARN) of the CMK. You cannot specify an alias. For help finding the key ID and ARN, see Finding the Key ID and ARN in the AWS Key Management Service Developer Guide.

_Required_: Yes

_Type_: String

_Minimum_: <code>1</code>

_Maximum_: <code>2048</code>

_Update requires_: [Replacement](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-replacement)

#### GranteePrincipal

The principal that is given permission to perform the operations that the grant permits. To specify the principal, use the Amazon Resource Name (ARN) of an AWS principal. Valid AWS principals include AWS accounts (root), IAM users, IAM roles, federated users, and assumed role users. For examples of the ARN syntax to use for specifying a principal, see AWS Identity and Access Management (IAM) in the Example ARNs section of the AWS General Reference.

_Required_: Yes

_Type_: String

_Minimum_: <code>1</code>

_Maximum_: <code>256</code>

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)

#### RetiringPrincipal

The principal that is given permission to retire the grant by using RetireGrant operation. To specify the principal, use the Amazon Resource Name (ARN) of an AWS principal. Valid AWS principals include AWS accounts (root), IAM users, IAM roles, federated users, and assumed role users. For examples of the ARN syntax to use for specifying a principal, see AWS Identity and Access Management (IAM) in the Example ARNs section of the AWS General Reference.

_Required_: No

_Type_: String

_Minimum_: <code>1</code>

_Maximum_: <code>256</code>

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)

#### Operations

A list of operations that the grant permits.

_Required_: Yes

_Type_: List of String

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)

#### Constraints

Allows a cryptographic operation only when the encryption context matches or includes the encryption context specified in this structure. For more information about encryption context, see Encryption Context in the AWS Key Management Service Developer Guide.

_Required_: No

_Type_: <a href="constraints.md">Constraints</a>

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)

#### GrantTokens

A list of grant tokens. For more information, see Grant Tokens in the AWS Key Management Service Developer Guide.

_Required_: No

_Type_: List of String

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)

#### Name

A friendly name for identifying the grant. Use this value to prevent the unintended creation of duplicate grants when retrying this request.

_Required_: No

_Type_: String

_Minimum_: <code>1</code>

_Maximum_: <code>256</code>

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)

## Return Values

### Fn::GetAtt

The `Fn::GetAtt` intrinsic function returns a value for a specified attribute of this type. The following are the available attributes and sample return values.

For more information about using the `Fn::GetAtt` intrinsic function, see [Fn::GetAtt](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/intrinsic-function-reference-getatt.html).

#### GrantId

Returns the <code>GrantId</code> value.
