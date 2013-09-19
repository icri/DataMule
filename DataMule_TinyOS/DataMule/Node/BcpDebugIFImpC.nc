configuration BcpDebugIFImpC {
  provides interface BcpDebugIF;
}
implementation {
  components BcpDebugIFImpP;

  BcpDebugIF = BcpDebugIFImpP;
}
