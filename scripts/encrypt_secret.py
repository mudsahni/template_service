import sys
from base64 import b64encode
from nacl import encoding, public

def encrypt(public_key: str, secret_value: str) -> str:
    """Encrypt a Unicode string using the public key."""
    public_key = public.PublicKey(public_key.encode("utf-8"), encoding.Base64Encoder())
    sealed_box = public.SealedBox(public_key)
    encrypted = sealed_box.encrypt(secret_value.encode("utf-8"))
    return b64encode(encrypted).decode("utf-8")

def main():
    if len(sys.argv) != 3:
        print("Usage: python script.py <base64_public_key> <secret_value>")
        sys.exit(1)

    public_key = sys.argv[1]
    secret_value = sys.argv[2]

    try:
        encrypted_secret = encrypt(public_key, secret_value)
        print(encrypted_secret)
    except Exception as e:
        print(f"Error: {str(e)}", file=sys.stderr)
        sys.exit(1)

if __name__ == "__main__":
    main()