# AWS::KMS::Grant Constraints

Allows a cryptographic operation only when the encryption context matches or includes the encryption context specified in this structure. For more information about encryption context, see Encryption Context in the AWS Key Management Service Developer Guide.

## Syntax

To declare this entity in your AWS CloudFormation template, use the following syntax:

### JSON

<pre>
{
    "<a href="#encryptioncontextsubset" title="EncryptionContextSubset">EncryptionContextSubset</a>" : <i>[ <a href="keyvaluepair.md">KeyValuePair</a>, ... ]</i>,
    "<a href="#encryptioncontextequals" title="EncryptionContextEquals">EncryptionContextEquals</a>" : <i>[ <a href="keyvaluepair.md">KeyValuePair</a>, ... ]</i>
}
</pre>

### YAML

<pre>
<a href="#encryptioncontextsubset" title="EncryptionContextSubset">EncryptionContextSubset</a>: <i>
      - <a href="keyvaluepair.md">KeyValuePair</a></i>
<a href="#encryptioncontextequals" title="EncryptionContextEquals">EncryptionContextEquals</a>: <i>
      - <a href="keyvaluepair.md">KeyValuePair</a></i>
</pre>

## Properties

#### EncryptionContextSubset

A list of key-value pairs that must be included in the encryption context of the cryptographic operation request. The grant allows the cryptographic operation only when the encryption context in the request includes the key-value pairs specified in this constraint, although it can include additional key-value pairs.

_Required_: No

_Type_: List of <a href="keyvaluepair.md">KeyValuePair</a>

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)

#### EncryptionContextEquals

A list of key-value pairs that must match the encryption context in the cryptographic operation request. The grant allows the operation only when the encryption context in the request is the same as the encryption context specified in this constraint.

_Required_: No

_Type_: List of <a href="keyvaluepair.md">KeyValuePair</a>

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)
