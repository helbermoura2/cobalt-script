#!/usr/bin/env python3
"""HX TV: substitui a tela inicial antiga por uma interface Android própria.

Aplica-se depois dos patches de identidade. Preserva o BrowsePresenter,
contas, reprodução e atualizador. Fallback voluntário: Modo clássico.
"""
from pathlib import Path
import argparse
import shutil

def substituir(arq,antes,depois):
    texto=arq.read_text(encoding="utf-8")
    quantas=texto.count(antes)
    if quantas!=1:
        raise RuntimeError(f"Marcador incompatível em {arq}: {antes!r}, {quantas} ocorrências")
    arq.write_text(texto.replace(antes,depois,1),encoding="utf-8")

def main():
    p=argparse.ArgumentParser()
    p.add_argument("upstream",type=Path)
    argumentos=p.parse_args()
    origem=Path(__file__).resolve().parent/"native-src"/"HxHomeScreen.java"
    destino=argumentos.upstream.resolve()/"smarttubetv/src/main/java/com/liskovsoft/smartyoutubetv2/tv/ui/browse/HxHomeScreen.java"
    if not origem.is_file() or not destino.parent.is_dir():
        raise RuntimeError("Arquivo nativo HX TV ou código-fonte Android ausente")
    shutil.copyfile(origem,destino)

    atividade=destino.parent/"BrowseActivity.java"
    substituir(atividade,"import android.os.Bundle;",
                       "import android.os.Bundle;\nimport android.content.Intent;\n"
                       "import com.liskovsoft.smartyoutubetv2.common.app.presenters.BrowsePresenter;")
    substituir(atividade,
        "    private static final String TAG = BrowseActivity.class.getSimpleName();",
        """    private static final String TAG = BrowseActivity.class.getSimpleName();
    private HxHomeScreen interfaceHx;
    private BrowsePresenter apresentadorHx;""")
    substituir(atividade,
        """        try {
            setContentView(R.layout.fragment_main);
        } catch (NoClassDefFoundError e) {
            // Failed resolution of: Landroidx/lifecycle/ViewTreeLifecycleOwner;
            MessageHelpers.showMessage(this, e.getMessage());
        }""",
        """        // A interface HX é a tela real, não uma camada por cima do SmartTube.
        if (getIntent().getBooleanExtra("hx_modo_classico", false)) {
            setContentView(R.layout.fragment_main);
            return;
        }
        try {
            interfaceHx = new HxHomeScreen(this);
            setContentView(interfaceHx);
            apresentadorHx = BrowsePresenter.instance(this);
            apresentadorHx.setView(interfaceHx);
            apresentadorHx.onViewInitialized();
        } catch (RuntimeException | NoClassDefFoundError problema) {
            android.util.Log.e("HX-TV", "Falha da tela HX; usando modo clássico", problema);
            interfaceHx = null;
            apresentadorHx = null;
            setContentView(R.layout.fragment_main);
            MessageHelpers.showMessage(this, "HX TV: modo clássico de segurança");
        }""")
    substituir(atividade,
        "    @Override\n    protected void initTheme() {",
        """    public void abrirModoClassico() {
        if (getIntent() != null) {
            getIntent().putExtra("hx_modo_classico", true);
        }
        recreate();
    }

    @Override
    public void onBackPressed() {
        if (interfaceHx != null && interfaceHx.voltarAoMenu()) {
            return;
        }
        super.onBackPressed();
    }

    @Override
    protected void onPause() {
        if (interfaceHx != null) interfaceHx.guardarFocoAntesDeSair();
        if (apresentadorHx != null) apresentadorHx.onViewPaused();
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (apresentadorHx != null) apresentadorHx.onViewResumed();
        if (interfaceHx != null) interfaceHx.restaurarFocoAposReproducao();
    }

    @Override
    public void onWindowFocusChanged(boolean recebeuFoco) {
        super.onWindowFocusChanged(recebeuFoco);
        if (recebeuFoco && interfaceHx != null) {
            interfaceHx.restaurarFocoAposReproducao();
        }
    }

    @Override
    protected void onDestroy() {
        if (apresentadorHx != null) apresentadorHx.onViewDestroyed();
        super.onDestroy();
    }

    @Override
    protected void initTheme() {""")
    codigo=atividade.read_text(encoding="utf-8")
    assert "setContentView(interfaceHx);" in codigo
    assert "R.layout.fragment_main" in codigo
    assert "abrirModoClassico()" in codigo
    assert "BrowsePresenter.instance(this)" in codigo
    print("Tela HX TV Android independente aplicada: navegação superior + modo clássico seguro.")

if __name__=="__main__":
    main()
