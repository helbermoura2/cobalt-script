package com.liskovsoft.smartyoutubetv2.tv.ui.browse;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Looper;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.liskovsoft.mediaserviceinterfaces.data.MediaGroup;
import com.liskovsoft.mediaserviceinterfaces.data.SearchOptions;
import com.liskovsoft.youtubeapi.service.YouTubeServiceManager;
import io.reactivex.disposables.Disposable;
import com.liskovsoft.smartyoutubetv2.common.app.models.data.BrowseSection;
import com.liskovsoft.smartyoutubetv2.common.app.models.data.SettingsGroup;
import com.liskovsoft.smartyoutubetv2.common.app.models.data.SettingsItem;
import com.liskovsoft.smartyoutubetv2.common.app.models.data.Video;
import com.liskovsoft.smartyoutubetv2.common.app.models.data.VideoGroup;
import com.liskovsoft.smartyoutubetv2.common.app.models.errors.ErrorFragmentData;
import com.liskovsoft.smartyoutubetv2.common.app.presenters.BrowsePresenter;
import com.liskovsoft.smartyoutubetv2.common.app.presenters.SearchPresenter;
import com.liskovsoft.smartyoutubetv2.common.misc.AppDataSourceManager;
import com.liskovsoft.smartyoutubetv2.common.app.views.BrowseView;
import com.liskovsoft.smartyoutubetv2.common.prefs.MainUIData;
import com.liskovsoft.smartyoutubetv2.common.utils.ClickbaitRemover;
import com.liskovsoft.smartyoutubetv2.tv.R;
import com.liskovsoft.smartyoutubetv2.tv.ui.playback.PlaybackActivity;
import com.liskovsoft.smartyoutubetv2.tv.ui.widgets.ContornoNeon;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

/**
 * Tela inicial independente HX TV. Sem BrowseFragment / WebView / máscara.
 * Compartilha apenas os dados, contas e a reprodução do SmartTube.
 */
public final class HxHomeScreen extends FrameLayout implements BrowseView {
    private static final int PRETO=0xFF000000, PAINEL=0x52141414;
    private static final int GELO=0xFFE7E7E7, TEXTO=0xFFFFFFFF, SECUNDARIO=0xFFB5B5B5;
    private static final int CIANO=0xFF8DEAFF;
    private int colunasInscricoes=4;
    private static final int FOCO=0xAA565656;
    private final BrowsePresenter dados;
    private final List<BrowseSection> secoes=new ArrayList<>();
    private final LinkedHashMap<String,LinearLayout> fileiras=new LinkedHashMap<>();
    private final LinkedHashMap<String,List<Video>> grupos=new LinkedHashMap<>();
    private final LinkedHashMap<String,Integer> limitesPorFileira=new LinkedHashMap<>();
    // Registro da home: não repetir o mesmo vídeo em várias fileiras recomendadas.
    private final LinkedHashMap<String,LinkedHashSet<String>> idsNaTela=new LinkedHashMap<>();
    private final LinearLayout menu;
    private final List<View> primeiraFileira=new ArrayList<>();
    private final LinearLayout pagina;
    private final ScrollView rolagem;
    private LinearLayout reservaPiP;
    private final PlaybackActivity.HxPipObserver hxPipObserver=ativo->{
        if(isAttachedToWindow())post(this::atualizarReservaPiP);
    };
    private final ProgressBar carregando;
    private LinearLayout estadoSecao;
    private HorizontalScrollView esqueletosSecao;
    private TextView mensagemEstadoSecao, tentarSecao;
    private Runnable checagemSecaoVazia;
    // Agrupa mudanças de catálogo durante refresh para não mostrar
    // uma única miniatura antes das demais ao voltar do player.
    private final List<VideoGroup> loteAtualizacao=new ArrayList<>();
    private Runnable finalizarLoteAtualizacao;
    private TextView tituloDestaque, detalhesDestaque, seloDestaque, tituloSelecionado;
    private TextView resumoSecao;
    private View acoesTopo;
    private ImageView imagemDestaque;
    private View primeiroCartao;
    private Video videoDestaque;
    private int secaoAtual=-1;
    // Guarda a posição real do cursor, inclusive ao abrir outro Activity de vídeo.
    private final LinkedHashMap<String,View> cartoesPorId=new LinkedHashMap<>();
    private String ultimoVideoFocado;
    private View ultimoControleFocado;
    private int ultimaSecaoFocada=-1;
    private boolean focoPendente;
    private boolean estavaNoMenu;
    private boolean voltarDaReproducao;
    private View destaqueFocavel;
    private String grupoPrincipalId;
    // Quando os vídeos são carregados de modo assíncrono, entrar no conteúdo
    // deve funcionar mesmo antes de o primeiro card existir.
    private boolean entrarNoConteudoPendente;
    private LinearLayout painelLateral;
    private TextView marcaNav, subtituloNav, pesquisaNav, atualizarNav, classicoNav, alternarNav;
    private boolean menuAberto=true;
    private ValueAnimator animacaoNav;
    private final List<TextView> itensMenu=new ArrayList<>();
    // HOME Horizon: cards estáveis 16:9, foco rápido sem reflow de layout.
    private static final int LARGURA_CAPA_HOME=205;
    private static final int ALTURA_CAPA_HOME=218;
    private final List<Disposable> buscasAssuntos=new ArrayList<>();
    private int geracaoPagina=0, proximoAssunto=0;
    private boolean exploracaoPronta=false, hubAoVivoPronto=false;
    private final LinkedHashMap<String,Video> transmissões=new LinkedHashMap<>();
    private final LinkedHashMap<Integer,LinkedHashMap<String,Video>> livesPorTema=new LinkedHashMap<>();
    private final List<TextView> filtrosDeLive=new ArrayList<>();
    private TextView statusDeLive;
    private int filtroLiveSelecionado=-1, rodadaLives=0;
    private boolean livesPendentes;
    private Runnable destaquePendente;
    private String ultimoDestaqueId;
    private static final String[][] LIVE_ASSUNTOS={
        {"Notícias","jornal notícias brasil ao vivo agora"},
        {"Esportes","esportes futebol jogos ao vivo hoje"},
        {"Games","live games gameplay streaming ao vivo agora"},
        {"Música","rádio música show ao vivo transmissão"},
        {"Podcasts","podcast entrevista ao vivo neste momento"},
        {"Natureza","câmera ao vivo natureza animais"},
        {"Tecnologia","tecnologia eventos ao vivo transmissão"},
        {"Mundo","live news world streaming now"}
    };
    private static final int MAX_VIDEOS_POR_FILEIRA=56; // compatibilidade do catálogo; crescimento é progressivo
    // Fontes reais: cada assunto pesquisa pelo próprio serviço de conteúdo
    // do aplicativo. Resultados, capas e autoria vêm da fonte, nunca são gerados.
    private static final String[][] ASSUNTOS={
        {"Cinema e trailers","trailers filmes cinema lançamentos brasil"},
        {"Séries e estreias","trailer séries estreias novidades"},
        {"Tecnologia","tecnologia novidades análises dispositivos"},
        {"Inteligência artificial","inteligência artificial novidades ferramentas"},
        {"Ciência","ciência descobertas pesquisas"},
        {"Astronomia e espaço","astronomia NASA universo descobertas"},
        {"Documentários","documentário completo história natureza"},
        {"História","história curiosidades civilizações documentário"},
        {"Futebol","futebol melhores momentos campeonato brasileiro"},
        {"Esportes","esportes melhores momentos notícias"},
        {"Música","música lançamentos videoclipes brasileiros"},
        {"Shows ao vivo","show ao vivo música apresentações"},
        {"Games","jogos gameplay lançamentos 2026"},
        {"Humor","comédia humor vídeos engraçados"},
        {"Natureza","natureza animais vida selvagem documentário"},
        {"Viagens","viagens destinos passeios mundo"},
        {"Culinária","receitas gastronomia cozinha"},
        {"Automóveis","carros testes lançamentos automobilismo"},
        {"Finanças","economia mercado finanças atualidades"},
        {"Notícias","notícias brasil mundo hoje"},
        {"Podcasts","podcasts entrevistas brasil"},
        {"Educação","aprender aulas conhecimento"},
        {"Programação","programação desenvolvimento software tutoriais"},
        {"Arquitetura e design","arquitetura interiores design casas"},
        {"Saúde e bem-estar","saúde atividade física bem estar"},
        {"Animações e anime","anime animação trailers novidades"},
        {"Infantil e família","vídeos educativos para crianças família"},
        {"Ao vivo agora","ao vivo transmissões brasil"}
    };

    private int dp(float valor){ return Math.round(getResources().getDisplayMetrics().density*valor); }

    private GradientDrawable fundo(int cor, int contorno, int raio){
        GradientDrawable forma=new GradientDrawable();
        forma.setColor(cor);
        forma.setCornerRadius(dp(raio));
        if(contorno!=0)forma.setStroke(dp(1.4f),contorno);
        return forma;
    }
    // Vidro neutro: transparencia alfa de verdade e brilho branco discreto.
    // Nao aplicar blur ao proprio View: isso embaçaria textos e icones.
    private GradientDrawable vidro(int cor, int borda, int raio){
        GradientDrawable forma=new GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                new int[]{0x26FFFFFF,cor,0x20000000});
        forma.setCornerRadius(dp(raio));
        forma.setStroke(dp(1),borda);
        return forma;
    }
    private void adicionarDuracao(FrameLayout capa,Video video){
        if(capa==null||video==null||video.isLive||video.isUpcoming)return;
        long duracao=video.getDurationMs();
        if(duracao<=0)return; // Sem duração conhecida: não estimar.
        long segundos=Math.max(1,(duracao+500L)/1000L);
        long horas=segundos/3600L;
        long minutos=(segundos%3600L)/60L;
        long resto=segundos%60L;
        String rotulo=horas>0
                ?String.format(java.util.Locale.ROOT,"%d:%02d:%02d",horas,minutos,resto)
                :String.format(java.util.Locale.ROOT,"%d:%02d",minutos,resto);
        TextView etiqueta=texto(rotulo,11,0xFFFFFFFF,true);
        etiqueta.setGravity(Gravity.CENTER);
        etiqueta.setPadding(dp(7),dp(3),dp(7),dp(3));
        etiqueta.setBackground(vidro(0xB0000000,0x90D6D6D6,7));
        etiqueta.setContentDescription("Duração: "+rotulo);
        FrameLayout.LayoutParams lp=new FrameLayout.LayoutParams(-2,-2,
                Gravity.END|Gravity.BOTTOM);
        lp.setMargins(0,0,dp(8),dp(8));
        capa.addView(etiqueta,lp);
    }
    private boolean estaNasInscricoes(){
        return secaoAtual>=0 && secaoAtual<secoes.size()
                && secoes.get(secaoAtual).getId()==MediaGroup.TYPE_SUBSCRIPTIONS;
    }
    private TextView texto(CharSequence valor,int tamanho,int cor,boolean negrito){
        TextView t=new TextView(getContext());
        t.setText(valor);
        t.setTextSize(tamanho);
        t.setTextColor(cor);
        t.setGravity(Gravity.CENTER_VERTICAL);
        t.setTypeface(Typeface.create(negrito?"sans-serif-medium":"sans-serif",
                negrito?Typeface.BOLD:Typeface.NORMAL));
        return t;
    }
    private void margem(View v,int a,int b,int c,int d){
        v.setPadding(dp(a),dp(b),dp(c),dp(d));
    }
    private void foco(View v){
        v.setFocusable(true);
        v.setBackground(vidro(0x4E151515,0x58A0A0A0,14));
        v.setOnFocusChangeListener((item,selecionado)->{
            item.setBackground(vidro(selecionado?0x99606060:0x4E151515,
                    selecionado?0xFFFFFFFF:0x58A0A0A0,14));
            if(Build.VERSION.SDK_INT>=21)item.setElevation(dp(selecionado?4:0));
        });
    }
    private void executar(Runnable tarefa){
        if(Looper.myLooper()==Looper.getMainLooper())tarefa.run();
        else post(tarefa);
    }

    public HxHomeScreen(Context contexto){
        super(contexto);
        dados=BrowsePresenter.instance(contexto);
        setBackgroundColor(PRETO);
        setFocusableInTouchMode(true);

        // HX TV Premium: trilho lateral nativo. Nenhum botão interativo sob
        // o Picture-in-Picture/miniplayer, que aparece no canto superior direito.
        LinearLayout estrutura=new LinearLayout(contexto);
        estrutura.setOrientation(LinearLayout.HORIZONTAL);
        addView(new FundoBlackGlass(contexto),new FrameLayout.LayoutParams(-1,-1));
        addView(estrutura,new FrameLayout.LayoutParams(-1,-1));

        LinearLayout lateral=new LinearLayout(contexto);
        lateral.setOrientation(LinearLayout.VERTICAL);
        lateral.setBackground(new GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                new int[]{0x40161E2B,0x24080D16,0x70000000}));
        margem(lateral,10,17,10,10);
        painelLateral=lateral;
        estrutura.addView(lateral,new LinearLayout.LayoutParams(dp(171),-1));

        TextView marca=texto("HX TV",30,TEXTO,true);
        marca.setTypeface(Typeface.create("sans-serif-black",Typeface.BOLD_ITALIC));
        marca.setShadowLayer(dp(11),dp(5),0,0xB0439FFF);
        marcaNav=marca;
        marca.setLetterSpacing(0.06f);
        margem(marca,9,0,0,0);
        lateral.addView(marca);
        TextView lema=texto("A SUA EXPERIÊNCIA",9,SECUNDARIO,true);
        subtituloNav=lema;
        lema.setLetterSpacing(0.12f);
        margem(lema,9,0,0,16);
        lateral.addView(lema);

        TextView alternar=texto("‹  RECOLHER MENU",11,GELO,true);
        alternarNav=alternar;
        alternar.setContentDescription("Recolher ou expandir menu lateral");
        margem(alternar,10,9,6,9);
        foco(alternar);
        iconeNativo(alternar,android.R.drawable.ic_menu_sort_by_size);
        alternar.setOnClickListener(v->setPainelAberto(!menuAberto));
        LinearLayout.LayoutParams toggleLp=new LinearLayout.LayoutParams(-1,dp(36));
        toggleLp.bottomMargin=dp(7);
        alternar.setBackground(vidro(0x8F19202D,0x88B2BCCB,14));
        alternar.setOnFocusChangeListener((v,ativo)->{
            v.setBackground(vidro(ativo?0xB4265369:0x8F19202D,0x88B2BCCB,14));
            pintarContornoNeon(v,ativo,true,14,0);
        });
        lateral.addView(alternar,toggleLp);

        ScrollView navegacao=new ScrollView(contexto);
        navegacao.setVerticalScrollBarEnabled(false);
        navegacao.setFillViewport(false);
        lateral.addView(navegacao,new LinearLayout.LayoutParams(-1,0,1));
        menu=new LinearLayout(contexto);
        menu.setGravity(Gravity.TOP);
        menu.setOrientation(LinearLayout.VERTICAL);
        navegacao.addView(menu,new ScrollView.LayoutParams(-1,-2));

        TextView pesquisa=texto("⌕  PESQUISAR",12,TEXTO,true);
        pesquisaNav=pesquisa;
        margem(pesquisa,13,10,9,10);
        foco(pesquisa);
        iconeNativo(pesquisa,android.R.drawable.ic_menu_search);
        pesquisa.setOnClickListener(v->
                SearchPresenter.instance(getContext()).startSearch(null));
        // Pesquisa, Atualizar e Modo clássico entram no MESMO ScrollView das categorias.

        TextView atualizar=texto("↻  ATUALIZAR",12,GELO,true);
        atualizarNav=atualizar;
        margem(atualizar,13,10,9,10);
        foco(atualizar);
        iconeNativo(atualizar,android.R.drawable.ic_popup_sync);
        atualizar.setContentDescription("Verificar atualizações do HX TV");
        atualizar.setOnClickListener(v->
                AppDataSourceManager.verificarAtualizacaoHx(getContext()));
        // Sem rodapé fixo: rolagem contínua.

        TextView antigo=texto("▤  MODO CLÁSSICO",11,SECUNDARIO,true);
        classicoNav=antigo;
        margem(antigo,11,10,3,10);
        foco(antigo);
        iconeNativo(antigo,android.R.drawable.ic_menu_view);
        antigo.setOnClickListener(v->{
            if(getContext() instanceof BrowseActivity)
                ((BrowseActivity)getContext()).abrirModoClassico();
        });
        // Modo clássico acompanha a rolagem.
        for(TextView item:new TextView[]{pesquisa,atualizar,antigo}){
            item.setOnFocusChangeListener((v,ativo)->{
                v.setBackground(vidro(ativo?0xA3616161:0x55161616,
                        0x70B7B7B7,15));
                pintarContornoNeon(v,ativo,true,15,0);
                Drawable d=item.getCompoundDrawables()[0];
                if(d==null)d=item.getCompoundDrawables()[1];
                if(d!=null)d.setTint(ativo?TEXTO:GELO);
                if(ativo)setPainelAberto(true);
            });
        }

        LinearLayout coluna=new LinearLayout(contexto);
        coluna.setOrientation(LinearLayout.VERTICAL);
        estrutura.addView(coluna,new LinearLayout.LayoutParams(0,-1,1));

        LinearLayout topo=new LinearLayout(contexto);
        topo.setGravity(Gravity.CENTER_VERTICAL);
        margem(topo,13,5,15,3);
        coluna.addView(topo,new LinearLayout.LayoutParams(-1,dp(67)));
        LinearLayout identidade=new LinearLayout(contexto);
        identidade.setOrientation(LinearLayout.VERTICAL);
        identidade.setGravity(Gravity.CENTER_VERTICAL);
        topo.addView(identidade,new LinearLayout.LayoutParams(0,-1,1));
        tituloSelecionado=texto("HX TV",24,TEXTO,true);
        tituloSelecionado.setSingleLine(true);
        tituloSelecionado.setEllipsize(TextUtils.TruncateAt.END);
        identidade.addView(tituloSelecionado,new LinearLayout.LayoutParams(-1,-2));
        resumoSecao=texto("",11,SECUNDARIO,false);
        resumoSecao.setSingleLine(true);
        resumoSecao.setVisibility(View.GONE);
        identidade.addView(resumoSecao);

        LinearLayout acoes=new LinearLayout(contexto);
        acoes.setGravity(Gravity.CENTER_VERTICAL);
        acoesTopo=acoes;
        topo.addView(acoes);
        adicionarAcaoTopo(acoes,"Pesquisar",android.R.drawable.ic_menu_search,
                ()->SearchPresenter.instance(getContext()).startSearch(null));
        adicionarAcaoTopo(acoes,"Pesquisa por voz",android.R.drawable.ic_btn_speak_now,
                ()->SearchPresenter.instance(getContext()).startVoice());
        adicionarAcaoTopo(acoes,"Notificações",
                com.liskovsoft.smartyoutubetv2.common.R.drawable.icon_notification,
                ()->dados.selectSection(MediaGroup.TYPE_NOTIFICATIONS));
        adicionarAcaoTopo(acoes,"Configurações",android.R.drawable.ic_menu_preferences,
                ()->dados.selectSection(MediaGroup.TYPE_SETTINGS));

        // Quando há vídeo em mini-janela, reservar a zona superior direita.
        // A TV decide a posição real do PiP: esta faixa é apenas proteção do catálogo.
        reservaPiP=new LinearLayout(contexto);
        reservaPiP.setOrientation(LinearLayout.VERTICAL);
        reservaPiP.setGravity(Gravity.CENTER_VERTICAL);
        reservaPiP.setPadding(dp(20),dp(4),dp(230),dp(4));
        reservaPiP.setBackground(vidro(0x520C0C0C,0x6FA6A6A6,15));
        TextView tituloMini=texto("▶  VÍDEO EM MINI-JANELA",11,TEXTO,true);
        reservaPiP.addView(tituloMini);
        TextView notaMini=texto("Continue navegando",10,SECUNDARIO,false);
        margem(notaMini,0,4,0,0);
        reservaPiP.addView(notaMini);
        reservaPiP.setVisibility(View.GONE);
        coluna.addView(reservaPiP,new LinearLayout.LayoutParams(-1,dp(76)));

        rolagem=new ScrollView(contexto);
        rolagem.setFillViewport(false);
        rolagem.setClipToPadding(false);
        rolagem.setVerticalScrollBarEnabled(false);
        rolagem.setDescendantFocusability(ViewGroup.FOCUS_AFTER_DESCENDANTS);
        coluna.addView(rolagem,new LinearLayout.LayoutParams(-1,0,1));
        pagina=new LinearLayout(contexto);
        pagina.setOrientation(LinearLayout.VERTICAL);
        margem(pagina,10,7,13,16);
        pagina.setClipChildren(false);
        pagina.setClipToPadding(false);
        rolagem.addView(pagina,new ScrollView.LayoutParams(-1,-2));
        pagina.addOnLayoutChangeListener((view,left,top,right,bottom,oldLeft,oldTop,oldRight,oldBottom)->{
            int largura=right-left-pagina.getPaddingLeft()-pagina.getPaddingRight();
            if(largura<=0)return;
            int colunas=largura<dp(640)?3:4;
            if(colunas==colunasInscricoes)return;
            colunasInscricoes=colunas;
            int geracao=geracaoPagina;
            post(()->{
                if(geracao!=geracaoPagina || !estaNasInscricoes())return;
                for(Map.Entry<String,List<Video>> grupo:grupos.entrySet())
                    desenharGradeInscricoes(grupo.getKey(),grupo.getValue(),
                            grupo.getKey().equals(grupoPrincipalId));
            });
        });
        TextView inicio=texto("ESCOLHA UMA CATEGORIA À ESQUERDA",15,SECUNDARIO,true);
        margem(inicio,0,24,0,20);
        pagina.addView(inicio);

        carregando=new ProgressBar(contexto);
        FrameLayout.LayoutParams progresso=new FrameLayout.LayoutParams(
                dp(42),dp(42),Gravity.CENTER);
        addView(carregando,progresso);
        carregando.setVisibility(View.GONE);
        post(this::atualizarReservaPiP);
    }

    private void adicionarAcaoTopo(LinearLayout barra,String nome,int recurso,Runnable acao){
        ImageView botao=new ImageView(getContext());
        botao.setImageResource(recurso);
        botao.setColorFilter(TEXTO);
        botao.setPadding(dp(6),dp(5),dp(6),dp(5));
        botao.setContentDescription(nome);
        botao.setFocusable(true);
        botao.setBackground(vidro(0xAE161C28,0x8C9BA6B9,9));
        botao.setOnFocusChangeListener((v,ativo)->v.setBackground(
                vidro(ativo?0xC1265369:0xAE161C28,ativo?CIANO:0x8C9BA6B9,9)));
        botao.setOnClickListener(v->acao.run());
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(dp(34),dp(31));
        lp.leftMargin=dp(7);
        barra.addView(botao,lp);
    }

    private void pintarContornoNeon(View alvo,boolean ativo,boolean intenso,int raio,int borda){
        Drawable atual=alvo.getForeground();
        if(ativo && atual instanceof ContornoNeon && ((ContornoNeon)atual).isIntenso()==intenso)return;
        alvo.setForeground(ativo ? new ContornoNeon(alvo,dp(raio),intenso,false)
                : fundo(0x00000000,borda,raio));
    }

    private static final class FundoBlackGlass extends View {
        private final Paint pintura=new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Path superior=new Path(), inferior=new Path();
        private Shader luzSuperior, luzInferior;

        FundoBlackGlass(Context contexto){super(contexto);}

        @Override protected void onSizeChanged(int w,int h,int anteriorW,int anteriorH){
            super.onSizeChanged(w,h,anteriorW,anteriorH);
            if(w==0||h==0)return;
            superior.reset();
            superior.moveTo(0,h*.045f);
            superior.cubicTo(w*.20f,-h*.16f,w*.27f,h*.26f,w*.44f,h*.07f);
            superior.cubicTo(w*.55f,-h*.065f,w*.72f,-h*.12f,w,h*.015f);
            superior.cubicTo(w*.65f,-h*.04f,w*.55f,h*.11f,w*.44f,h*.125f);
            superior.cubicTo(w*.28f,h*.24f,w*.16f,-h*.06f,0,h*.13f);
            superior.close();
            inferior.reset();
            inferior.moveTo(0,h*.87f);
            inferior.cubicTo(w*.27f,h*.66f,w*.44f,h*1.22f,w*.78f,h*.94f);
            inferior.lineTo(w,h*.75f);
            inferior.cubicTo(w*.76f,h*1.37f,w*.47f,h*1.08f,w*.18f,h*.97f);
            inferior.lineTo(0,h);
            inferior.close();
            luzSuperior=new LinearGradient(0,0,0,h*.18f,
                    new int[]{0xFF04060C,0xFF151A23,0xFF090B10,PRETO},null,Shader.TileMode.CLAMP);
            luzInferior=new LinearGradient(0,h*.82f,w*.53f,h,
                    new int[]{PRETO,0xFF131922,0xFF090B10,PRETO},null,Shader.TileMode.CLAMP);
        }

        @Override protected void onDraw(Canvas tela){
            tela.drawColor(PRETO);
            pintura.setShader(luzSuperior);
            tela.drawPath(superior,pintura);
            pintura.setShader(luzInferior);
            tela.drawPath(inferior,pintura);
        }
    }

    private void atualizarReservaPiP(){
        if(reservaPiP==null)return;
        boolean emMini=PlaybackActivity.isHxMiniPlayerActive();
        reservaPiP.setVisibility(emMini?View.VISIBLE:View.GONE);
        acoesTopo.setVisibility(emMini?View.INVISIBLE:View.VISIBLE);
    }

    @Override public void onWindowFocusChanged(boolean possuiFoco){
        super.onWindowFocusChanged(possuiFoco);
        if(possuiFoco){
            atualizarReservaPiP();
            postDelayed(this::atualizarReservaPiP,350);
        }
    }

    /** Os símbolos são tipográficos; o nome integral permanece acessível. */
    private void iconeNativo(TextView alvo,int recurso){
        try{
            Drawable desenho=getResources().getDrawable(recurso).mutate();
            desenho.setBounds(0,0,dp(21),dp(21));
            desenho.setTint(GELO);
            alvo.setCompoundDrawables(desenho,null,null,null);
            alvo.setCompoundDrawablePadding(dp(10));
        }catch(android.content.res.Resources.NotFoundException ignorado){}
    }
    private void posicionarNav(TextView alvo,String rotulo){
        if(alvo==null)return;
        Drawable icone=alvo.getCompoundDrawables()[0];
        if(icone==null)icone=alvo.getCompoundDrawables()[1];
        if(menuAberto){
            alvo.setText(rotulo);
            alvo.setGravity(Gravity.CENTER_VERTICAL);
            margem(alvo,12,5,5,5);
            if(icone!=null)alvo.setCompoundDrawables(icone,null,null,null);
            alvo.setCompoundDrawablePadding(dp(10));
        }else{
            alvo.setText(icone==null?"•":"");
            alvo.setGravity(Gravity.CENTER);
            margem(alvo,0,3,0,3);
            if(icone!=null)alvo.setCompoundDrawables(null,icone,null,null);
            alvo.setCompoundDrawablePadding(0);
        }
    }
    private int ordemMenu(int id){
        switch(id){
            case MediaGroup.TYPE_HOME:return 0;
            case MediaGroup.TYPE_SUBSCRIPTIONS:return 1;
            case MediaGroup.TYPE_USER_PLAYLISTS:return 2;
            case MediaGroup.TYPE_HISTORY:return 3;
            case MediaGroup.TYPE_MUSIC:return 4;
            case MediaGroup.TYPE_LIVE:return 5;
            case MediaGroup.TYPE_NEWS:return 6;
            case MediaGroup.TYPE_CHANNEL_UPLOADS:return 7;
            case MediaGroup.TYPE_SETTINGS:return 8;
            default:return 9;
        }
    }

    private View itemCategoriaAtual(){
        if(itensMenu.isEmpty())return null;
        return itensMenu.get(Math.max(0,Math.min(secaoAtual,itensMenu.size()-1)));
    }

    private void atualizarLegendasMenu(){
        for(int i=0;i<itensMenu.size()&&i<secoes.size();i++){
            TextView aba=itensMenu.get(i);
            posicionarNav(aba,secoes.get(i).getTitle());
        }
        if(marcaNav!=null){
            marcaNav.setText(menuAberto?"HX TV":"HX");
            marcaNav.setTextSize(menuAberto?30:21);
            margem(marcaNav,menuAberto?9:0,0,0,0);
        }
        if(subtituloNav!=null)subtituloNav.setVisibility(menuAberto?View.VISIBLE:View.GONE);
        posicionarNav(alternarNav,menuAberto?"RECOLHER":"MENU");
        posicionarNav(pesquisaNav,"PESQUISAR");
        posicionarNav(atualizarNav,"ATUALIZAR");
        posicionarNav(classicoNav,"MODO CLÁSSICO");
    }
    private void setPainelAberto(boolean abrir){
        if(painelLateral==null||menuAberto==abrir)return;
        menuAberto=abrir;
        if(animacaoNav!=null)animacaoNav.cancel();
        atualizarLegendasMenu();
        int de=painelLateral.getLayoutParams().width;
        int ate=dp(abrir?171:56);
        animacaoNav=ValueAnimator.ofInt(de,ate);
        animacaoNav.setDuration(190);
        animacaoNav.addUpdateListener(a->{
            ViewGroup.LayoutParams p=painelLateral.getLayoutParams();
            p.width=(int)a.getAnimatedValue();
            painelLateral.setLayoutParams(p);
        });
        animacaoNav.start();
    }

    private void renovarMenu(){
        boolean restaurarMenu=menu.hasFocus();
        menu.removeAllViews();
        itensMenu.clear();
        for(int i=0;i<secoes.size();i++){
            final int indice=i;
            String nome=secoes.get(i).getTitle();
            TextView aba=texto(nome,12,TEXTO,true);
            aba.setContentDescription(nome);
            int recurso=secoes.get(i).getResId();
            iconeNativo(aba,recurso>0?recurso:android.R.drawable.ic_menu_view);
            itensMenu.add(aba);
            aba.setSingleLine(true);
            aba.setEllipsize(TextUtils.TruncateAt.MARQUEE);
            aba.setMarqueeRepeatLimit(-1);
            margem(aba,13,8,10,8);
            aba.setFocusable(true);
            aba.setOnFocusChangeListener((v,ativo)->{
                aba.setSelected(ativo);
                pintarCategoria(aba,ativo,secaoAtual==indice);
                if(ativo)setPainelAberto(true);
            });
            aba.setOnClickListener(v->abrirSecao(indice));
            // O DPAD chega primeiro ao item com foco. Capturar aqui evita que
            // o ScrollView lateral engula RIGHT no Início.
            aba.setOnKeyListener((v,key,event)->{
                if(event.getAction()==KeyEvent.ACTION_DOWN &&
                        key==KeyEvent.KEYCODE_DPAD_RIGHT){
                    if(secaoAtual!=indice)abrirSecao(indice);
                    return entrarNoConteudo();
                }
                return false;
            });
        }
        List<Integer> ordem=new ArrayList<>();
        for(int i=0;i<itensMenu.size();i++)ordem.add(i);
        Collections.sort(ordem,(a,b)->Integer.compare(ordemMenu(secoes.get(a).getId()),
                ordemMenu(secoes.get(b).getId())));
        for(int indice:ordem){
            LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,dp(34));
            lp.bottomMargin=dp(5);
            menu.addView(itensMenu.get(indice),lp);
        }
        for(TextView acao:new TextView[]{pesquisaNav,atualizarNav,classicoNav}){
            LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,dp(34));
            lp.topMargin=dp(acao==pesquisaNav?17:2);
            lp.bottomMargin=dp(5);
            menu.addView(acao,lp);
        }
        atualizarLegendasMenu();
        marcarMenu();
        View categoria=itemCategoriaAtual();
        if(restaurarMenu && categoria!=null)post(categoria::requestFocus);
    }
    /** Apenas o item com foco ganha CONTORNO. Categoria atual usa preenchimento discreto. */
    private void pintarCategoria(TextView aba, boolean focada, boolean atual){
        aba.setBackground(vidro(focada?0xE42A586E:
                atual?0xA2292E34:0x8C161C28,
                atual?0xB0919AA3:0x8697A3B8,16));
        pintarContornoNeon(aba,focada,true,16,0);
        aba.setElevation(focada?dp(5):0);
        if(Build.VERSION.SDK_INT>=28){
            aba.setOutlineAmbientShadowColor(CIANO);
            aba.setOutlineSpotShadowColor(CIANO);
        }
        aba.setTextColor(focada?TEXTO:atual?0xFFE2EAF3:SECUNDARIO);
        Drawable icone=aba.getCompoundDrawables()[0];
        if(icone==null)icone=aba.getCompoundDrawables()[1];
        if(icone!=null)icone.setTint(focada?TEXTO:atual?GELO:SECUNDARIO);
    }
    private void marcarMenu(){
        for(int i=0;i<itensMenu.size();i++){
            TextView aba=itensMenu.get(i);
            pintarCategoria(aba,aba.isFocused(),i==secaoAtual);
        }
    }

    private boolean estaNaPrimeiraFileira(View focado){
        View atual=focado;
        while(atual!=null){
            if(primeiraFileira.contains(atual))return true;
            if(!(atual.getParent() instanceof View))break;
            atual=(View)atual.getParent();
        }
        return false;
    }

    private boolean focarCategoriaAtual(){
        View aba=itemCategoriaAtual();
        if(aba==null)return false;
        setPainelAberto(true);
        if(aba.requestFocus()){
            estavaNoMenu=true;
            ultimoControleFocado=aba;
            marcarMenu();
            return true;
        }
        return false;
    }

    // Foco explícito: LATERAL ↔ CARDS. No Android TV, deixar a busca
    // automática decidir esse salto faz o cursor sumir atrás do miniplayer.
    private boolean primeiraColunaDaFaixa(View focado){
        if(focado==null)return false;
        for(LinearLayout faixa:fileiras.values()){
            if(faixa.getChildCount()>0 && faixa.getChildAt(0)==focado)return true;
        }
        return focado==primeiroCartao && primeiroCartao!=null;
    }

    /** Localiza sempre um card conectado. Não confia em referência antiga. */
    private View primeiroCardValido(){
        for(LinearLayout faixa:fileiras.values()){
            for(int i=0;i<faixa.getChildCount();i++){
                View card=faixa.getChildAt(i);
                if(estaNasInscricoes() && card instanceof LinearLayout)
                    card=((LinearLayout)card).getChildAt(0);
                if(card!=null&&card.isAttachedToWindow()&&card.isShown()
                        &&card.isFocusable())return card;
            }
        }
        return null;
    }

    private boolean entrarNosCards(){
        View card=primeiroCardValido();
        if(card!=null&&card.requestFocus()){
            primeiroCartao=card;
            entrarNoConteudoPendente=false;
            return true;
        }
        entrarNoConteudoPendente=true;
        return true;
    }

    private boolean entrarNoConteudo(){
        // Toda a navegação da esquerda começa por um destino visual seguro.
        // Não usa ultimoControleFocado: depois da troca de seção pode ser
        // uma referência inválida da tela anterior.
        View alvo=destaqueFocavel;
        if(estaNasConfiguracoes())alvo=primeiroCartao;
        if(alvo==null || !alvo.isAttachedToWindow() || !alvo.isShown())
            alvo=primeiroCardValido();
        if(alvo!=null && alvo.requestFocus()){
            entrarNoConteudoPendente=false;
            return true;
        }
        entrarNoConteudoPendente=true;
        return true;
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent evento){
        if(evento.getAction()==KeyEvent.ACTION_DOWN){
            View atual=findFocus();
            int tecla=evento.getKeyCode();
            if(tecla==KeyEvent.KEYCODE_DPAD_RIGHT && menu.hasFocus()
                    && !itensMenu.contains(atual)){
                return entrarNoConteudo();
            }
            if(tecla==KeyEvent.KEYCODE_DPAD_LEFT &&
                    atual!=null && primeiraColunaDaFaixa(atual)){
                if(focarCategoriaAtual())return true;
            }
            if(!estaNaHome() && !estaNasInscricoes()
                    && tecla==KeyEvent.KEYCODE_DPAD_UP &&
                    atual!=null && estaNaPrimeiraFileira(atual)){
                if(focarCategoriaAtual())return true;
            }
        }
        return super.dispatchKeyEvent(evento);
    }

    // Mensagem real para seções sem resultado, em vez de uma tela vazia.
    // Nunca inventa vídeos, inscrições ou conteúdo de conta não sincronizada.
    private void mostrarCarregamentoSecao(){
        if(secaoAtual<0||secaoAtual>=secoes.size())return;
        if(estadoSecao!=null && estadoSecao.getParent()==pagina)return;
        String nome=secoes.get(secaoAtual).getTitle();
        estadoSecao=new LinearLayout(getContext());
        estadoSecao.setOrientation(LinearLayout.VERTICAL);
        estadoSecao.setGravity(Gravity.CENTER_VERTICAL);
        estadoSecao.setBackground(vidro(0x70151515,0x77B0B0B0,17));
        estadoSecao.setPadding(dp(25),dp(18),dp(25),dp(17));
        LinearLayout.LayoutParams bloco=new LinearLayout.LayoutParams(-1,-2);
        bloco.topMargin=dp(12);
        bloco.bottomMargin=dp(8);
        pagina.addView(estadoSecao,bloco);
        TextView cabecalho=texto("HX TV  /  "+nome.toUpperCase(java.util.Locale.ROOT),12,GELO,true);
        estadoSecao.addView(cabecalho);
        mensagemEstadoSecao=texto("Buscando conteúdo disponível...",14,TEXTO,false);
        LinearLayout.LayoutParams msg=new LinearLayout.LayoutParams(-1,-2);
        msg.topMargin=dp(9);
        estadoSecao.addView(mensagemEstadoSecao,msg);
        // Skeleton de fileira: geometria estável, sem capas ou vídeos fictícios.
        // Não recebe foco nem inicia animações, preservando a fluidez da TV.
        esqueletosSecao=new HorizontalScrollView(getContext());
        esqueletosSecao.setHorizontalScrollBarEnabled(false);
        esqueletosSecao.setFocusable(false);
        LinearLayout.LayoutParams eps=new LinearLayout.LayoutParams(-1,dp(137));
        eps.topMargin=dp(15);
        estadoSecao.addView(esqueletosSecao,eps);
        LinearLayout trilhoEspera=new LinearLayout(getContext());
        trilhoEspera.setOrientation(LinearLayout.HORIZONTAL);
        esqueletosSecao.addView(trilhoEspera,
                new HorizontalScrollView.LayoutParams(-2,-1));
        for(int i=0;i<4;i++){
            LinearLayout carta=new LinearLayout(getContext());
            carta.setOrientation(LinearLayout.VERTICAL);
            LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(dp(174),-1);
            cp.rightMargin=dp(12);
            trilhoEspera.addView(carta,cp);
            View imagem=new View(getContext());
            imagem.setBackground(new GradientDrawable(
                    GradientDrawable.Orientation.TL_BR,
                    new int[]{0xFF292929,0xFF1B1B1B,0xFF101010}));
            carta.addView(imagem,new LinearLayout.LayoutParams(-1,dp(96)));
            View tituloEspera=new View(getContext());
            tituloEspera.setBackground(vidro(0x603D3D3D,0x688F8F8F,5));
            LinearLayout.LayoutParams linha1=new LinearLayout.LayoutParams(
                    dp(i%2==0?128:148),dp(9));
            linha1.topMargin=dp(9);
            carta.addView(tituloEspera,linha1);
            View subtituloEspera=new View(getContext());
            subtituloEspera.setBackground(vidro(0x50303030,0x60909090,5));
            LinearLayout.LayoutParams linha2=new LinearLayout.LayoutParams(
                    dp(i%2==0?80:102),dp(7));
            linha2.topMargin=dp(5);
            carta.addView(subtituloEspera,linha2);
        }
        tentarSecao=texto("↻  TENTAR NOVAMENTE",12,GELO,true);
        tentarSecao.setGravity(Gravity.CENTER_VERTICAL);
        tentarSecao.setBackground(vidro(0x98505050,0xB0D5D5D5,12));
        tentarSecao.setPadding(dp(14),dp(6),dp(14),dp(6));
        LinearLayout.LayoutParams retry=new LinearLayout.LayoutParams(dp(188),dp(35));
        retry.topMargin=dp(12);
        estadoSecao.addView(tentarSecao,retry);
        tentarSecao.setVisibility(View.GONE);
        tentarSecao.setFocusable(true);
        tentarSecao.setOnClickListener(v->{
            if(secaoAtual<0||secaoAtual>=secoes.size())return;
            abrirSecao(secaoAtual);
        });
        if(checagemSecaoVazia!=null)removeCallbacks(checagemSecaoVazia);
        final int geracao=geracaoPagina;
        final LinearLayout quadro=estadoSecao;
        checagemSecaoVazia=()->{
            checagemSecaoVazia=null;
            if(geracao!=geracaoPagina||quadro!=estadoSecao ||
                    quadro.getParent()!=pagina || grupoPrincipalId!=null)return;
            carregando.setVisibility(View.GONE);
            mensagemEstadoSecao.setText("Nenhum vídeo recebido nesta seção. "+
                    "Ela pode estar vazia ou ainda não sincronizada.");
            if(esqueletosSecao!=null)esqueletosSecao.setVisibility(View.GONE);
            tentarSecao.setVisibility(View.VISIBLE);
        };
        postDelayed(checagemSecaoVazia,4700);
    }
    private void limparEstadoSecao(){
        if(checagemSecaoVazia!=null){
            removeCallbacks(checagemSecaoVazia);
            checagemSecaoVazia=null;
        }
        if(estadoSecao!=null && estadoSecao.getParent()==pagina)
            pagina.removeView(estadoSecao);
        estadoSecao=null;
        esqueletosSecao=null;
        mensagemEstadoSecao=null;
        tentarSecao=null;
    }
    private void limparConteudo(){
        limparEstadoSecao();
        if(finalizarLoteAtualizacao!=null){
            removeCallbacks(finalizarLoteAtualizacao);
            finalizarLoteAtualizacao=null;
        }
        loteAtualizacao.clear();
        geracaoPagina++;
        for(Disposable d:buscasAssuntos)if(d!=null&&!d.isDisposed())d.dispose();
        buscasAssuntos.clear();
        exploracaoPronta=false;
        hubAoVivoPronto=false;
        transmissões.clear();
        livesPorTema.clear();
        filtrosDeLive.clear();
        statusDeLive=null;
        filtroLiveSelecionado=-1;
        rodadaLives++;
        livesPendentes=false;
        if(destaquePendente!=null)removeCallbacks(destaquePendente);
        destaquePendente=null;
        ultimoDestaqueId=null;
        proximoAssunto=0;
        pagina.removeAllViews();
        fileiras.clear();
        grupos.clear();
        resumoSecao.setVisibility(View.GONE);
        limitesPorFileira.clear();
        idsNaTela.clear();
        primeiroCartao=null;
        destaqueFocavel=null;
        grupoPrincipalId=null;
        primeiraFileira.clear();
        cartoesPorId.clear();
        videoDestaque=null;
        tituloDestaque=null;
        detalhesDestaque=null;
        imagemDestaque=null;
        seloDestaque=null;
        rolagem.scrollTo(0,0);
    }
    private void abrirSecao(int indice){
        if(indice<0||indice>=secoes.size())return;
        // Ao trocar de categoria, o foco deve seguir a nova categoria.
        if(secaoAtual!=indice){
            entrarNoConteudoPendente=false;
            ultimoVideoFocado=null;
            ultimoControleFocado=null;
            focoPendente=false;
        }
        secaoAtual=indice;
        tituloSelecionado.setText(secoes.get(indice).getTitle());
        limparConteudo();
        marcarMenu();
        mostrarCarregamentoSecao();
        boolean configuracoes=estaNasConfiguracoes();
        if(configuracoes)entrarNoConteudoPendente=true;
        dados.onSectionFocused(secoes.get(indice).getId());
        if(configuracoes && primeiroCartao==null)
            desenharConfiguracoes(AppDataSourceManager.instance().getSettingItems(getContext()));
        if(configuracoes)entrarNoConteudo();
    }
    private boolean estaNasConfiguracoes(){
        return secaoAtual>=0 && secaoAtual<secoes.size()
                && secoes.get(secaoAtual).getId()==MediaGroup.TYPE_SETTINGS;
    }
    private boolean estaNaHome(){
        return secaoAtual>=0 && secaoAtual<secoes.size()
                && secoes.get(secaoAtual).getId()==MediaGroup.TYPE_HOME;
    }
    private boolean estaNaAreaAoVivo(){
        return secaoAtual>=0 && secaoAtual<secoes.size()
                && secoes.get(secaoAtual).getId()==MediaGroup.TYPE_LIVE;
    }
    private boolean podeExibirHubAoVivo(){
        return estaNaHome()||estaNaAreaAoVivo();
    }
    // Pesquisa transversal por vários temas, para não depender de um único
    // grupo de transmissões ao vivo sugerido pelo algoritmo.
    private void adicionarHubAoVivo(){
        if(hubAoVivoPronto||!podeExibirHubAoVivo()||videoDestaque==null)return;
        hubAoVivoPronto=true;
        LinearLayout area=new LinearLayout(getContext());
        area.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);
        lp.bottomMargin=dp(10);
        pagina.addView(area,Math.min(2,pagina.getChildCount()),lp);
        TextView titulo=texto("●  AO VIVO AGORA",14,TEXTO,true);
        area.addView(titulo);
        statusDeLive=texto("Buscando transmissões disponíveis...",10,SECUNDARIO,false);
        LinearLayout.LayoutParams t=new LinearLayout.LayoutParams(-1,-2);
        t.topMargin=dp(2);t.bottomMargin=dp(9);
        area.addView(statusDeLive,t);
        HorizontalScrollView scroll=new HorizontalScrollView(getContext());
        scroll.setHorizontalScrollBarEnabled(false);
        area.addView(scroll);
        LinearLayout trilho=new LinearLayout(getContext());
        trilho.setOrientation(LinearLayout.HORIZONTAL);
        scroll.addView(trilho,new HorizontalScrollView.LayoutParams(-2,-2));
        fileiras.put("hx/hub-ao-vivo",trilho);
        for(int i=-1;i<=LIVE_ASSUNTOS.length;i++){
            final int indice=i;
            String nome=i<0?"Todos":i==LIVE_ASSUNTOS.length?"↻ Atualizar":LIVE_ASSUNTOS[i][0];
            TextView chip=texto(nome,12,TEXTO,true);
            chip.setGravity(Gravity.CENTER);
            chip.setContentDescription("Filtrar transmissões: "+nome);
            foco(chip);
            LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(dp(i<0?76:104),dp(37));
            p.rightMargin=dp(7);
            trilho.addView(chip,p);
            filtrosDeLive.add(chip);
            chip.setOnFocusChangeListener((v,ativo)->{
                pintarFiltroLive(chip,ativo,indice==filtroLiveSelecionado);
                if(ativo)setPainelAberto(false);
            });
            chip.setOnClickListener(v->{
                if(indice==LIVE_ASSUNTOS.length){
                    iniciarBuscaLives();
                    return;
                }
                filtroLiveSelecionado=indice;
                for(int j=0;j<filtrosDeLive.size();j++)
                    pintarFiltroLive(filtrosDeLive.get(j),
                            filtrosDeLive.get(j).isFocused(),j-1==indice);
                atualizarFileiraLive();
            });
            pintarFiltroLive(chip,false,i==-1);
        }
        iniciarBuscaLives();
    }
    private void iniciarBuscaLives(){
        if(!podeExibirHubAoVivo())return;
        rodadaLives++;
        final int rodada=rodadaLives,geracao=geracaoPagina;
        transmissões.clear();
        livesPorTema.clear();
        LinearLayout antiga=fileiras.get("hx/live/multitemas");
        if(antiga!=null&&!antiga.hasFocus())antiga.removeAllViews();
        if(statusDeLive!=null)statusDeLive.setText("Procurando lives de 8 assuntos...");
        long intervalo=estaNaHome()?900L:580L;
        for(int i=0;i<LIVE_ASSUNTOS.length;i++){
            final int indice=i;
            postDelayed(()->pesquisarLive(indice,geracao,rodada),
                    350L+(long)i*intervalo);
        }
        postDelayed(()->{
            if(geracao==geracaoPagina && rodada==rodadaLives
                    && transmissões.isEmpty() && statusDeLive!=null)
                statusDeLive.setText("Nenhuma transmissão ativa encontrada. "+
                        "Use Atualizar ou experimente outro assunto.");
        },12500);
    }
    private void pintarFiltroLive(TextView chip,boolean emFoco,boolean selecionado){
        chip.setBackground(vidro(emFoco?0xAA696969:
                selecionado?0x88505050:0x4E141414,
                emFoco?0xFFFFFFFF:selecionado?0xB0CDCDCD:0x649B9B9B,13));
        chip.setTextColor(emFoco?TEXTO:selecionado?GELO:SECUNDARIO);
    }
    private void atualizarFileiraLive(){
        if(!podeExibirHubAoVivo()||statusDeLive==null)return;
        List<Video> lista=new ArrayList<>();
        if(filtroLiveSelecionado<0)lista.addAll(transmissões.values());
        else{
            LinkedHashMap<String,Video> selecionadas=livesPorTema.get(filtroLiveSelecionado);
            if(selecionadas!=null)lista.addAll(selecionadas.values());
        }
        String rotulo=filtroLiveSelecionado<0?"Todos":LIVE_ASSUNTOS[filtroLiveSelecionado][0];
        if(lista.isEmpty()){
            statusDeLive.setText("Nenhuma live encontrada em "+rotulo+". Escolha outro tema.");
            LinearLayout anterior=fileiras.get("hx/live/multitemas");
            if(anterior!=null&&!anterior.hasFocus())anterior.removeAllViews();
            return;
        }
        statusDeLive.setText(lista.size()+" transmissões encontradas · "+rotulo);
        LinearLayout faixaAnterior=fileiras.get("hx/live/multitemas");
        if(faixaAnterior!=null && faixaAnterior.hasFocus()){
            // Não destruir os cards sob o controle enquanto resultados chegam.
            livesPendentes=true;
            return;
        }
        livesPendentes=false;
        desenharFileira("hx/live/multitemas","ASSISTA AO VIVO",lista,false);
    }
    private void pesquisarLive(int indice,int geracao,int rodada){
        if(geracao!=geracaoPagina||rodada!=rodadaLives
                ||!podeExibirHubAoVivo()||!isAttachedToWindow())return;
        final String consulta=LIVE_ASSUNTOS[indice][1];
        try{
            Disposable busca=YouTubeServiceManager.instance().getContentService()
                .getSearchObserve(consulta,
                    SearchOptions.FEATURE_LIVE | SearchOptions.TYPE_VIDEO)
                .subscribe(resultado->{
                    if(resultado==null)return;
                    List<Video> validos=new ArrayList<>();
                    for(MediaGroup grupo:resultado){
                        if(grupo==null||grupo.isEmpty())continue;
                        VideoGroup videos=VideoGroup.from(grupo);
                        if(videos.getVideos()==null)continue;
                        for(Video video:videos.getVideos()){
                            if(video!=null&&video.isLive&&!video.isLiveEnd
                                    &&!video.isUpcoming&&video.videoId!=null)
                                validos.add(video);
                        }
                    }
                    if(validos.isEmpty())return;
                    post(()->{
                        if(geracao!=geracaoPagina||rodada!=rodadaLives
                                ||!podeExibirHubAoVivo())return;
                        LinkedHashMap<String,Video> porTema=livesPorTema.get(indice);
                        if(porTema==null){
                            porTema=new LinkedHashMap<>();
                            livesPorTema.put(indice,porTema);
                        }
                        for(Video v:validos){
                            if(porTema.size()<28)porTema.put(v.videoId,v);
                            if(transmissões.size()<64)transmissões.put(v.videoId,v);
                        }
                        atualizarFileiraLive();
                    });
                },erro->android.util.Log.w("HX-Live",
                        "Falha isolada em "+LIVE_ASSUNTOS[indice][0],erro));
            buscasAssuntos.add(busca);
        }catch(RuntimeException e){
            android.util.Log.w("HX-Live","Pesquisa indisponível",e);
        }
    }

    private void adicionarExplorar(){
        if(exploracaoPronta||!estaNaHome()||videoDestaque==null)return;
        exploracaoPronta=true;
        LinearLayout painel=new LinearLayout(getContext());
        painel.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams bloco=new LinearLayout.LayoutParams(-1,-2);
        bloco.topMargin=dp(2);bloco.bottomMargin=dp(12);
        pagina.addView(painel,Math.min(4,pagina.getChildCount()),bloco);
        TextView titulo=texto("EXPLORE SEUS INTERESSES",14,TEXTO,true);
        titulo.setLetterSpacing(0.04f);
        painel.addView(titulo);
        TextView legenda=texto(ASSUNTOS.length+" assuntos, vídeos reais e novidades",10,SECUNDARIO,false);
        LinearLayout.LayoutParams sub=new LinearLayout.LayoutParams(-1,-2);
        sub.topMargin=dp(4);sub.bottomMargin=dp(12);
        painel.addView(legenda,sub);
        HorizontalScrollView deslocamento=new HorizontalScrollView(getContext());
        deslocamento.setHorizontalScrollBarEnabled(false);
        painel.addView(deslocamento);
        LinearLayout linha=new LinearLayout(getContext());
        linha.setOrientation(LinearLayout.HORIZONTAL);
        deslocamento.addView(linha,new HorizontalScrollView.LayoutParams(-2,-2));
        fileiras.put("hx/explorar",linha);
        for(String[] assunto:ASSUNTOS){
            TextView chip=texto("⌕  "+assunto[0],13,TEXTO,true);
            chip.setContentDescription("Explorar: "+assunto[0]);
            chip.setSingleLine(true);
            margem(chip,16,9,16,9);
            foco(chip);
            LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(-2,dp(38));
            cp.rightMargin=dp(9);
            linha.addView(chip,cp);
            chip.setOnFocusChangeListener((v,selecionado)->{
                chip.setBackground(fundo(selecionado?0xFF484848:0xFF191919,
                        selecionado?0xFFF1F7FF:0,12));
                if(selecionado)setPainelAberto(false);
            });
            chip.setOnClickListener(v->
                    SearchPresenter.instance(getContext()).startSearch(assunto[1]));
        }
        carregarProximosAssuntos(2);
    }
    private void carregarProximosAssuntos(int quantidade){
        if(!estaNaHome()||!exploracaoPronta)return;
        final int inicio=proximoAssunto;
        final int ate=Math.min(ASSUNTOS.length,inicio+quantidade);
        if(inicio>=ate)return;
        proximoAssunto=ate;
        final int geracao=geracaoPagina;
        for(int i=inicio;i<ate;i++){
            final int assuntoIndex=i;
            postDelayed(()->pesquisarAssunto(assuntoIndex,geracao),
                    1200L+(long)(i-inicio)*1100L);
        }
    }
    private void pesquisarAssunto(int index,int geracao){
        if(!isAttachedToWindow() || geracao!=geracaoPagina || !estaNaHome())return;
        final String titulo=ASSUNTOS[index][0], consulta=ASSUNTOS[index][1];
        try{
            Disposable busca=YouTubeServiceManager.instance()
                .getContentService().getSearchObserve(consulta,0)
                .subscribe(resultados->{
                    if(resultados==null)return;
                    for(MediaGroup resultado:resultados){
                        if(resultado==null||resultado.isEmpty())continue;
                        VideoGroup grupo=VideoGroup.from(resultado);
                        List<Video> videos=grupo.getVideos();
                        if(videos==null||videos.isEmpty())continue;
                        post(()->{
                            if(geracao!=geracaoPagina||!estaNaHome())return;
                            String chave="hx/tema/"+index;
                            if(fileiras.containsKey(chave))return;
                            desenharFileira(chave,titulo,videos,false);
                        });
                        break;
                    }
                },erro->{
                    // Falha isolada por tema: nunca interrompe a home.
                    android.util.Log.w("HX-Discover",titulo+": "+erro);
                });
            buscasAssuntos.add(busca);
        }catch(RuntimeException e){
            android.util.Log.w("HX-Discover","Busca indisponível: "+titulo,e);
        }
    }
    private String chave(VideoGroup g){
        return g.getId()+"/"+(g.getTitle()==null?"Vídeos":g.getTitle());
    }
    private void destaque(Video primeiro){
        LinearLayout exterior=new LinearLayout(getContext());
        exterior.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams vertical=new LinearLayout.LayoutParams(-1,-2);
        vertical.bottomMargin=dp(11);
        pagina.addView(exterior,0,vertical);

        TextView rotulo=texto("ESCOLHIDO PARA VOCÊ",10,GELO,true);
        rotulo.setLetterSpacing(0.15f);
        margem(rotulo,1,0,0,6);
        exterior.addView(rotulo);

        // Hero cinematográfico: uma imagem única com vinheta lateral,
        // e textos em área protegida. Não é um segundo player nem autoplay.
        FrameLayout quadro=new FrameLayout(getContext());
        quadro.setBackground(vidro(0x72131313,0x99C0C0C0,18));
        if(Build.VERSION.SDK_INT>=21)quadro.setClipToOutline(true);
        exterior.addView(quadro,new LinearLayout.LayoutParams(-1,dp(142)));
        destaqueFocavel=quadro;
        quadro.setFocusable(true);
        quadro.setClickable(true);
        quadro.setOnClickListener(v->{
            if(videoDestaque!=null){
                guardarFocoAntesDeSair();
                dados.onVideoItemClicked(videoDestaque);
            }
        });
        quadro.setOnFocusChangeListener((v,active)->{
            quadro.setForeground(fundo(0x00000000,active?0xFFF5F5F5:0,17));
            if(Build.VERSION.SDK_INT>=21)quadro.setElevation(dp(active?8:0));
            v.animate().cancel();
            v.animate().scaleX(active?1.01f:1f).scaleY(active?1.01f:1f)
                    .setDuration(165).start();
            if(active)setPainelAberto(false);
        });
        quadro.setOnKeyListener((v,key,event)->{
            if(event.getAction()==KeyEvent.ACTION_DOWN &&
                    key==KeyEvent.KEYCODE_DPAD_DOWN)return entrarNosCards();
            if(event.getAction()==KeyEvent.ACTION_DOWN &&
                    key==KeyEvent.KEYCODE_DPAD_LEFT)return focarCategoriaAtual();
            return false;
        });

        imagemDestaque=new ImageView(getContext());
        imagemDestaque.setScaleType(ImageView.ScaleType.CENTER_CROP);
        quadro.addView(imagemDestaque,new FrameLayout.LayoutParams(-1,-1));

        View vinheta=new View(getContext());
        vinheta.setBackground(new GradientDrawable(
                GradientDrawable.Orientation.LEFT_RIGHT,
                new int[]{0xF9000000,0xD9000000,0xA4000000,0x12000000}));
        quadro.addView(vinheta,new FrameLayout.LayoutParams(-1,-1));

        LinearLayout lateralTexto=new LinearLayout(getContext());
        lateralTexto.setOrientation(LinearLayout.VERTICAL);
        lateralTexto.setGravity(Gravity.CENTER_VERTICAL);
        margem(lateralTexto,20,9,10,9);
        LinearLayout area=new LinearLayout(getContext());
        area.setOrientation(LinearLayout.HORIZONTAL);
        quadro.addView(area,new FrameLayout.LayoutParams(-1,-1));
        area.addView(lateralTexto,new LinearLayout.LayoutParams(0,-1,58));
        area.addView(new View(getContext()),new LinearLayout.LayoutParams(0,-1,42));

        seloDestaque=texto("HX TV",10,GELO,true);
        seloDestaque.setLetterSpacing(0.13f);
        lateralTexto.addView(seloDestaque);

        tituloDestaque=texto("",19,TEXTO,true);
        tituloDestaque.setMaxLines(2);
        tituloDestaque.setEllipsize(TextUtils.TruncateAt.END);
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);
        lp.topMargin=dp(6);
        lateralTexto.addView(tituloDestaque,lp);

        detalhesDestaque=texto("",10,SECUNDARIO,false);
        detalhesDestaque.setMaxLines(1);
        detalhesDestaque.setEllipsize(TextUtils.TruncateAt.END);
        LinearLayout.LayoutParams mp=new LinearLayout.LayoutParams(-1,-2);
        mp.topMargin=dp(5);
        lateralTexto.addView(detalhesDestaque,mp);

        TextView indicacao=texto("▶  ASSISTIR",11,TEXTO,true);
        indicacao.setGravity(Gravity.CENTER);
        indicacao.setBackground(vidro(0xB0717171,0xFFF5F5F5,10));
        LinearLayout.LayoutParams hp=new LinearLayout.LayoutParams(dp(130),dp(30));
        hp.topMargin=dp(8);
        lateralTexto.addView(indicacao,hp);

        atualizarDestaque(primeiro);
    }
    private void agendarDestaque(Video video){
        if(video==null||video.videoId==null)return;
        if(video.videoId.equals(ultimoDestaqueId))return;
        if(destaquePendente!=null)removeCallbacks(destaquePendente);
        final int geracao=geracaoPagina;
        destaquePendente=()->{
            destaquePendente=null;
            if(geracao!=geracaoPagina||!isAttachedToWindow())return;
            ultimoDestaqueId=video.videoId;
            atualizarDestaque(video);
        };
        postDelayed(destaquePendente,170);
    }
    private void atualizarDestaque(Video video){
        if(video==null||tituloDestaque==null)return;
        videoDestaque=video;
        tituloDestaque.setText(video.getTitle());
        detalhesDestaque.setText(video.getSecondTitle());
        seloDestaque.setText(video.isLive?"●  AO VIVO":"HX TV  ·  DESTAQUE");
        Glide.with(getContext())
             .load(ClickbaitRemover.updateThumbnail(video,
                     MainUIData.instance(getContext()).getThumbQuality()))
             .diskCacheStrategy(DiskCacheStrategy.ALL)
              .transition(DrawableTransitionOptions.withCrossFade(240))
             .error(R.drawable.card_placeholder).into(imagemDestaque);
    }
    // Uma única aparição por vídeo entre fileiras da Home.
    // Transmissões no hub são exceção: a prioridade é localizar lives ativas.
    private List<Video> semDuplicarNaHome(String chave,List<Video> candidatos){
        if(!estaNaHome()||chave.startsWith("hx/live/"))return candidatos;
        LinkedHashSet<String> utilizados=new LinkedHashSet<>();
        for(Map.Entry<String,LinkedHashSet<String>> registro:idsNaTela.entrySet()){
            if(registro.getKey().equals(chave))break;
            utilizados.addAll(registro.getValue());
        }
        LinkedHashSet<String> atuais=new LinkedHashSet<>();
        List<Video> distintos=new ArrayList<>();
        for(Video video:candidatos){
            if(video==null)continue;
            String id=video.videoId;
            if(id==null||(!utilizados.contains(id)&&atuais.add(id)))
                distintos.add(video);
        }
        idsNaTela.put(chave,atuais);
        return distintos;
    }
    // Inscrições em grade, com três ou quatro colunas segundo a largura.
    private void desenharGradeInscricoes(String chave,List<Video> videos,boolean principal){
        LinearLayout grade=fileiras.get(chave);
        if(grade==null){
            grade=new LinearLayout(getContext());
            grade.setOrientation(LinearLayout.VERTICAL);
            grade.setClipChildren(false);
            pagina.addView(grade,new LinearLayout.LayoutParams(-1,-2));
            fileiras.put(chave,grade);
        }
        if(grade.hasFocus()){
            focoPendente=true;
            ultimaSecaoFocada=secaoAtual;
        }
        if(principal)primeiraFileira.clear();
        String prefixo=chave+"|";
        Iterator<String> referencias=cartoesPorId.keySet().iterator();
        while(referencias.hasNext()){
            if(referencias.next().startsWith(prefixo))referencias.remove();
        }
        grade.removeAllViews();
        List<Video> disponiveis=new ArrayList<>();
        for(Video video:videos)if(video!=null)disponiveis.add(video);
        Integer limiteAnterior=limitesPorFileira.get(chave);
        int limite=Math.min(disponiveis.size(),limiteAnterior==null?28:limiteAnterior);
        List<View> cartoes=new ArrayList<>(limite);
        final int colunas=colunasInscricoes;
        LinearLayout linha=null;
        for(int i=0;i<limite;i++){
            if(i%colunas==0){
                linha=new LinearLayout(getContext());
                linha.setClipChildren(false);
                linha.setGravity(Gravity.TOP);
                LinearLayout.LayoutParams faixa=new LinearLayout.LayoutParams(-1,-2);
                faixa.bottomMargin=dp(9);
                grade.addView(linha,faixa);
            }
            final int pos=i;
            Video video=disponiveis.get(i);
            String referencia=chave+"|"+(video.videoId==null?"id:"+video.id:"vid:"+video.videoId);
            View cartao=criarCartaoInscricao(video);
            cartoes.add(cartao);
            cartoesPorId.put(referencia,cartao);
            if(principal && i<colunas)primeiraFileira.add(cartao);
            if(principal && i==0)primeiroCartao=cartao;
            LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,-2,1);
            if(i%colunas!=0)lp.leftMargin=dp(9);
            linha.addView(cartao,lp);
            cartao.setOnFocusChangeListener((v,selecionado)->{
                v.animate().scaleX(selecionado?1.025f:1f)
                        .scaleY(selecionado?1.025f:1f).setDuration(130).start();
                v.setElevation(selecionado?dp(5):0);
                pintarContornoNeon(v,selecionado,true,15,0x85838C9F);
                if(!selecionado)return;
                ultimoVideoFocado=referencia;
                ultimoControleFocado=v;
                ultimaSecaoFocada=secaoAtual;
                estavaNoMenu=false;
                dados.onVideoItemSelected(video);
            });
            cartao.setOnKeyListener((v,key,event)->{
                if(event.getAction()!=KeyEvent.ACTION_DOWN)return false;
                int coluna=pos%colunas;
                if(key==KeyEvent.KEYCODE_DPAD_LEFT)
                    return coluna==0?focarCategoriaAtual():cartoes.get(pos-1).requestFocus();
                if(key==KeyEvent.KEYCODE_DPAD_RIGHT && coluna<colunas-1
                        && pos+1<limite)return cartoes.get(pos+1).requestFocus();
                if(key==KeyEvent.KEYCODE_DPAD_UP && pos>=colunas)
                    return cartoes.get(pos-colunas).requestFocus();
                if(key==KeyEvent.KEYCODE_DPAD_DOWN){
                    int proximaLinha=pos-coluna+colunas;
                    if(proximaLinha<limite)
                        return cartoes.get(Math.min(pos+colunas,limite-1)).requestFocus();
                    if(limite<disponiveis.size()){
                        limitesPorFileira.put(chave,limite+16);
                        desenharGradeInscricoes(chave,videos,principal);
                        return true;
                    }
                    dados.onScrollEnd(video);
                }
                return false;
            });
            cartao.setOnClickListener(v->{
                guardarFocoAntesDeSair();
                dados.onVideoItemClicked(video);
            });
            cartao.setOnLongClickListener(v->{
                dados.onVideoItemLongClicked(video);
                return true;
            });
        }
        if(linha!=null && limite%colunas!=0){
            for(int i=limite%colunas;i<colunas;i++){
                LinearLayout.LayoutParams espaco=new LinearLayout.LayoutParams(0,0,1);
                espaco.leftMargin=dp(9);
                linha.addView(new View(getContext()),espaco);
            }
        }
        atualizarResumoInscricoes();
        if(focoPendente && ultimaSecaoFocada==secaoAtual)post(this::tentarRestaurarFoco);
        if(entrarNoConteudoPendente)post(this::entrarNoConteudo);
    }

    private void atualizarResumoInscricoes(){
        LinkedHashSet<Video> unicos=new LinkedHashSet<>();
        for(List<Video> grupo:grupos.values())unicos.addAll(grupo);
        int lives=0;
        for(Video video:unicos)if(video!=null&&video.isLive&&!video.isLiveEnd&&!video.isUpcoming)lives++;
        android.text.SpannableString resumo=new android.text.SpannableString(
                "Últimos vídeos  •  ● "+lives+" ao vivo");
        int ponto=resumo.toString().indexOf('●');
        resumo.setSpan(new android.text.style.ForegroundColorSpan(0xFFFF414D),
                ponto,ponto+1,0);
        resumoSecao.setText(resumo);
        resumoSecao.setVisibility(View.VISIBLE);
    }

    private View criarCartaoInscricao(Video video){
        LinearLayout cartao=new LinearLayout(getContext());
        cartao.setOrientation(LinearLayout.VERTICAL);
        cartao.setFocusable(true);
        cartao.setBackground(vidro(0xE0232A38,0x85838C9F,15));
        cartao.setClipToOutline(true);
        if(Build.VERSION.SDK_INT>=28){
            cartao.setOutlineAmbientShadowColor(CIANO);
            cartao.setOutlineSpotShadowColor(CIANO);
        }
        cartao.setForeground(fundo(0x00000000,0x85838C9F,15));
        FrameLayout capa=new FrameLayout(getContext()){
            @Override protected void onMeasure(int largura,int altura){
                int medida=Math.round(MeasureSpec.getSize(largura)*0.50f);
                super.onMeasure(largura,MeasureSpec.makeMeasureSpec(medida,MeasureSpec.EXACTLY));
            }
        };
        capa.setBackground(fundo(0xFF151A25,0,15));
        capa.setClipToOutline(true);
        cartao.addView(capa,new LinearLayout.LayoutParams(-1,-2));
        TextView fallback=texto("HX TV",13,SECUNDARIO,true);
        fallback.setGravity(Gravity.CENTER);
        capa.addView(fallback,new FrameLayout.LayoutParams(-1,-1));
        ImageView imagem=new ImageView(getContext());
        imagem.setScaleType(ImageView.ScaleType.CENTER_CROP);
        capa.addView(imagem,new FrameLayout.LayoutParams(-1,-1));
        Glide.with(this)
                .load(ClickbaitRemover.updateThumbnail(video,MainUIData.instance(getContext()).getThumbQuality()))
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .error(android.R.color.transparent)
                .transition(DrawableTransitionOptions.withCrossFade(140))
                .into(imagem);
        if((video.isLive&&!video.isLiveEnd)||video.isUpcoming){
            TextView selo=texto(video.isUpcoming?"EM BREVE":"● AO VIVO",10,TEXTO,true);
            margem(selo,6,2,6,2);
            selo.setBackground(fundo(video.isUpcoming?0xE0353C49:0xF0D91829,0,8));
            FrameLayout.LayoutParams lp=new FrameLayout.LayoutParams(-2,-2,Gravity.END|Gravity.BOTTOM);
            lp.setMargins(0,0,dp(5),dp(4));
            capa.addView(selo,lp);
        }else adicionarDuracao(capa,video);

        LinearLayout detalhes=new LinearLayout(getContext());
        detalhes.setOrientation(LinearLayout.VERTICAL);
        margem(detalhes,9,3,8,6);
        cartao.addView(detalhes,new LinearLayout.LayoutParams(-1,-2));
        LinearLayout tituloLinha=new LinearLayout(getContext());
        detalhes.addView(tituloLinha,new LinearLayout.LayoutParams(-1,-2));
        TextView titulo=texto(video.getTitle(),12,TEXTO,true);
        titulo.setLines(2);
        titulo.setIncludeFontPadding(false);
        titulo.setGravity(Gravity.TOP|Gravity.START);
        titulo.setEllipsize(TextUtils.TruncateAt.END);
        tituloLinha.addView(titulo,new LinearLayout.LayoutParams(0,-2,1));
        TextView opcoes=texto("⋮",15,TEXTO,true);
        opcoes.setGravity(Gravity.TOP|Gravity.CENTER_HORIZONTAL);
        opcoes.setContentDescription("Opções do vídeo");
        opcoes.setOnClickListener(v->dados.onVideoItemLongClicked(video));
        opcoes.setFocusable(false);
        tituloLinha.addView(opcoes,new LinearLayout.LayoutParams(dp(15),-1));
        String canal=video.getAuthor();
        CharSequence metadados=video.getSecondTitle();
        LinearLayout canalLinha=new LinearLayout(getContext());
        canalLinha.setGravity(Gravity.CENTER_VERTICAL);
        detalhes.addView(canalLinha,new LinearLayout.LayoutParams(-1,-2));
        LinearLayout linhasCanal=new LinearLayout(getContext());
        linhasCanal.setOrientation(LinearLayout.VERTICAL);
        if(!TextUtils.isEmpty(canal)){
            String inicial=new String(Character.toChars(canal.codePointAt(0)));
            TextView avatar=texto(inicial.toUpperCase(java.util.Locale.ROOT),10,TEXTO,true);
            avatar.setGravity(Gravity.CENTER);
            avatar.setBackground(vidro(0xCB274D72,0x8EB1D2ED,20));
            LinearLayout.LayoutParams circulo=new LinearLayout.LayoutParams(dp(22),dp(22));
            circulo.rightMargin=dp(6);
            canalLinha.addView(avatar,circulo);
            TextView autor=texto(canal,10,GELO,false);
            autor.setSingleLine(true);
            autor.setEllipsize(TextUtils.TruncateAt.END);
            linhasCanal.addView(autor);
            if(metadados!=null && metadados.toString().startsWith(canal)){
                metadados=metadados.subSequence(canal.length(),metadados.length())
                        .toString().replaceFirst("^[\\s•·]+","");
            }
        }
        TextView info=texto(metadados,9,SECUNDARIO,false);
        info.setSingleLine(true);
        info.setEllipsize(TextUtils.TruncateAt.END);
        linhasCanal.addView(info);
        canalLinha.addView(linhasCanal,new LinearLayout.LayoutParams(0,-2,1));
        return cartao;
    }

    private void desenharFileira(String chave,String nome,List<Video> videos,boolean principal){
        if(estaNasInscricoes()){
            desenharGradeInscricoes(chave,videos,principal);
            return;
        }
        final List<Video> exibidos=semDuplicarNaHome(chave,videos);
        if(exibidos==null||exibidos.isEmpty())return;
        LinearLayout faixa=fileiras.get(chave);
        if(faixa==null){
            LinearLayout bloco=new LinearLayout(getContext());
            bloco.setOrientation(LinearLayout.VERTICAL);
            LinearLayout.LayoutParams bp=new LinearLayout.LayoutParams(-1,-2);
            bp.bottomMargin=dp(17);
            if(chave.startsWith("hx/live/")&&podeExibirHubAoVivo())
                pagina.addView(bloco,Math.min(3,pagina.getChildCount()),bp);
            else pagina.addView(bloco,bp);
            TextView cabecalho=texto(principal?"PARA VOCÊ":nome,15,TEXTO,true);
            margem(cabecalho,1,0,0,9);bloco.addView(cabecalho);
            HorizontalScrollView deslizar=new HorizontalScrollView(getContext());
            deslizar.setHorizontalScrollBarEnabled(false);
            deslizar.setClipToPadding(false);
            deslizar.setClipChildren(false);
            bloco.addView(deslizar);
            faixa=new LinearLayout(getContext());
            faixa.setOrientation(LinearLayout.HORIZONTAL);
            faixa.setClipChildren(false);
            deslizar.addView(faixa);
            fileiras.put(chave,faixa);
        }
        // O catálogo pode atualizar enquanto o cursor está numa miniatura.
        // A view antiga será removida; agendar foco para a nova view equivalente.
        if(faixa.hasFocus()){
            focoPendente=true;
            ultimaSecaoFocada=secaoAtual;
        }
        if(principal)primeiraFileira.clear();
        final LinearLayout faixaAtual=faixa;
        faixa.removeAllViews();
        final int limite=Math.min(exibidos.size(),
                limitesPorFileira.containsKey(chave)?limitesPorFileira.get(chave):20);
        int n=0;
        for(Video video:exibidos){
            if(n>=limite)break;
            final int pos=n++;
            final String referencia=chave+"|"+(video.videoId==null?
                    "id:"+video.id:"vid:"+video.videoId);
            final boolean cartaz=estaNaHome();
            final boolean descoberto=chave.startsWith("hx/tema/");
            LinearLayout cartao=new LinearLayout(getContext());
            cartao.setOrientation(LinearLayout.VERTICAL);
            foco(cartao);
            // O contorno arredondado agora recorta a miniatura e seus filhos.
            if(Build.VERSION.SDK_INT>=21)cartao.setClipToOutline(true);
            cartao.setForeground(fundo(0x00000000,0x77888888,17));
            LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(
                    dp(cartaz?LARGURA_CAPA_HOME:225),dp(cartaz?ALTURA_CAPA_HOME:226));
            cp.rightMargin=dp(11);faixa.addView(cartao,cp);
            // Selo sobre a miniatura verdadeira, sem mascarar a interface.
            FrameLayout capa=new FrameLayout(getContext());
            cartao.addView(capa,new LinearLayout.LayoutParams(-1,
                    dp(cartaz?115:123)));
            // Fundo editorial de baixo custo até a imagem real chegar.
            // Se a capa falhar, mostrar HX em vez de um retângulo cinza vazio.
            GradientDrawable fundoCapa=new GradientDrawable(
                    GradientDrawable.Orientation.TL_BR,
                    new int[]{0xFF292929,0xFF181818,0xFF090909});
            fundoCapa.setCornerRadius(dp(14));
            capa.setBackground(fundoCapa);
            if(Build.VERSION.SDK_INT>=21)capa.setClipToOutline(true);
            TextView seloEspera=texto("▶  HX TV",13,0xFFA8A8A8,true);
            seloEspera.setGravity(Gravity.CENTER);
            seloEspera.setLetterSpacing(.12f);
            capa.addView(seloEspera,new FrameLayout.LayoutParams(-1,-1));
            ImageView miniatura=new ImageView(getContext());
            miniatura.setScaleType(ImageView.ScaleType.CENTER_CROP);
            capa.addView(miniatura,new FrameLayout.LayoutParams(-1,-1));
            if(video.isLive && !video.isLiveEnd && !video.isUpcoming){
                TextView aoVivo=texto("● AO VIVO",10,TEXTO,true);
                margem(aoVivo,9,5,9,5);
                aoVivo.setBackground(vidro(0xB0A32B46,0xCCED8198,9));
                FrameLayout.LayoutParams selo=new FrameLayout.LayoutParams(
                        -2,-2,Gravity.TOP|Gravity.END);
                selo.setMargins(0,dp(8),dp(8),0);
                capa.addView(aoVivo,selo);
            }else if(video.isUpcoming){
                TextView emBreve=texto("EM BREVE",10,TEXTO,true);
                margem(emBreve,9,5,9,5);
                emBreve.setBackground(vidro(0xA6414141,0xB8CECECE,9));
                FrameLayout.LayoutParams selo=new FrameLayout.LayoutParams(
                        -2,-2,Gravity.TOP|Gravity.END);
                selo.setMargins(0,dp(8),dp(8),0);
                capa.addView(emBreve,selo);
            }
            Glide.with(getContext())
                    .load(ClickbaitRemover.updateThumbnail(video,
                            MainUIData.instance(getContext()).getThumbQuality()))
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .error(android.R.color.transparent)
                    .into(miniatura);
            adicionarDuracao(capa,video);
            TextView nomeCartao=texto(video.getTitle(),13,TEXTO,true);
            nomeCartao.setMaxLines(3);
            nomeCartao.setLineSpacing(dp(1),1.08f);
            nomeCartao.setGravity(Gravity.TOP|Gravity.START);
            nomeCartao.setEllipsize(TextUtils.TruncateAt.END);
            margem(nomeCartao,9,8,9,0);
            cartao.addView(nomeCartao,new LinearLayout.LayoutParams(-1,0,1));
            TextView dadosCartao=texto(video.getSecondTitle(),10,SECUNDARIO,false);
            dadosCartao.setSingleLine(true);
            dadosCartao.setEllipsize(TextUtils.TruncateAt.END);
            margem(dadosCartao,8,0,8,7);
            cartao.addView(dadosCartao,new LinearLayout.LayoutParams(-1,dp(27)));
            cartoesPorId.put(referencia,cartao);
            if(principal)primeiraFileira.add(cartao);
            cartao.setOnFocusChangeListener((v,selecionado)->{
                if(selecionado)setPainelAberto(false);
                v.setBackground(vidro(selecionado?0xA9606060:0x46111111,
                        selecionado?0xFFFFFFFF:0x76BABABA,17));
                // Borda em primeiro plano: a capa nao encobre mais o contorno.
                pintarContornoNeon(v,selecionado,true,17,0x77888888);
                v.animate().cancel();
                v.animate().scaleX(selecionado?1.018f:1f)
                        .scaleY(selecionado?1.018f:1f)
                        .translationY(selecionado?-dp(2):0)
                        .setDuration(160).start();
                if(Build.VERSION.SDK_INT>=21)
                    v.setElevation(dp(selecionado?4:0));
                if(selecionado){
                    ultimoVideoFocado=referencia;
                    ultimoControleFocado=v;
                    ultimaSecaoFocada=secaoAtual;
                    estavaNoMenu=false;
                }
                if(!selecionado && chave.startsWith("hx/live/") && livesPendentes){
                    postDelayed(()->{
                        if(!faixaAtual.hasFocus() && livesPendentes)
                            atualizarFileiraLive();
                    },250);
                }
                nomeCartao.setEllipsize(TextUtils.TruncateAt.END);
                if(selecionado){
                    dados.onVideoItemSelected(video);
                    if(estaNaHome())agendarDestaque(video);
                    if(pos>=limite-3 && limite<exibidos.size()){
                        // Carrega mais capas desta mesma fileira conforme necessário.
                        // Redesenho adiado para preservar o vídeo focado pelo controle.
                        int novo=Math.min(exibidos.size(),limite+12);
                        Integer ja=limitesPorFileira.get(chave);
                        if(ja==null||ja<novo){
                            limitesPorFileira.put(chave,novo);
                            postDelayed(()->{
                                if(faixaAtual.isAttachedToWindow())
                                    desenharFileira(chave,nome,exibidos,principal);
                            },220);
                        }
                    }else if(pos>=limite-1){
                        if(descoberto)carregarProximosAssuntos(3);
                        else if(!chave.startsWith("hx/live/"))
                            dados.onScrollEnd(video);
                    }
                }
            });
            // Rotas explícitas na grade; não dependem de FocusFinder atravessar
            // múltiplos HorizontalScrollViews em diferentes versões Android.
            cartao.setOnKeyListener((v,key,event)->{
                if(event.getAction()!=KeyEvent.ACTION_DOWN)return false;
                if(key==KeyEvent.KEYCODE_DPAD_LEFT && pos==0)
                    return focarCategoriaAtual();
                if(key==KeyEvent.KEYCODE_DPAD_UP && principal && !estaNaHome())
                    return destaqueFocavel!=null
                            && destaqueFocavel.requestFocus();
                if(key==KeyEvent.KEYCODE_DPAD_DOWN
                        || (key==KeyEvent.KEYCODE_DPAD_UP && !principal)){
                    List<LinearLayout> linhas=new ArrayList<>(fileiras.values());
                    // A ordem visual muda quando resultados ao vivo chegam.
                    // Ordenar pelas posições reais em vez de ordem de download.
                    Collections.sort(linhas,(x,y)->
                            Integer.compare(posicaoFaixa(x),posicaoFaixa(y)));
                    int atualLinha=linhas.indexOf(faixaAtual);
                    int destino=atualLinha+(key==KeyEvent.KEYCODE_DPAD_DOWN?1:-1);
                    if(destino>=0 && destino<linhas.size()){
                        LinearLayout outra=linhas.get(destino);
                        if(outra.getChildCount()>0){
                            View proximo=outra.getChildAt(
                                    Math.min(pos,outra.getChildCount()-1));
                            if(proximo!=null&&proximo.requestFocus())return true;
                        }
                    }else if(key==KeyEvent.KEYCODE_DPAD_UP
                            &&destaqueFocavel!=null){
                        return destaqueFocavel.requestFocus();
                    }
                }
                return false;
            });
            cartao.setOnClickListener(v->{
                guardarFocoAntesDeSair();
                dados.onVideoItemClicked(video);
            });
            cartao.setOnLongClickListener(v->{
                dados.onVideoItemLongClicked(video);return true;
            });
            if(primeiroCartao==null)primeiroCartao=cartao;
        }
        if(focoPendente && ultimaSecaoFocada==secaoAtual){
            post(this::tentarRestaurarFoco);
        }
        if(entrarNoConteudoPendente && (primeiroCartao!=null || destaqueFocavel!=null)){
            post(this::entrarNoConteudo);
        }
    }
    private int posicaoFaixa(LinearLayout faixa){
        View bloco=faixa;
        while(bloco.getParent() instanceof View
                && bloco.getParent()!=pagina)bloco=(View)bloco.getParent();
        if(bloco.getParent()!=pagina)return 100000;
        return pagina.indexOfChild(bloco);
    }
    private void desenharGrupo(VideoGroup grupo){
        if(grupo==null)return;
        if(grupo.getAction()==VideoGroup.ACTION_REPLACE){
            limparConteudo();
            mostrarCarregamentoSecao();
        }
        String id=chave(grupo);
        if(grupo.getAction()==VideoGroup.ACTION_REMOVE){
            grupos.remove(id);
            idsNaTela.remove(id);
            return;
        }
        List<Video> videos=grupos.get(id);
        if(videos==null){videos=new ArrayList<>();grupos.put(id,videos);}
        if(grupo.getAction()==VideoGroup.ACTION_SYNC)videos.clear();
        if(grupo.getAction()==VideoGroup.ACTION_PREPEND){
            List<Video> novo=new ArrayList<>(grupo.getVideos());
            for(Video v:videos)if(!novo.contains(v))novo.add(v);
            videos.clear();videos.addAll(novo);
        }else{
            for(Video v:grupo.getVideos())if(!videos.contains(v))videos.add(v);
        }
        if(videos.isEmpty())return;
        // A primeira lista com vídeos REAIS vira o destaque. Grupos vazios
        // podem chegar antes dos vídeos e não podem consumir o slot principal.
        if(grupoPrincipalId==null)grupoPrincipalId=id;
        boolean principal=id.equals(grupoPrincipalId);
        if(principal&&videoDestaque==null){
            limparEstadoSecao();
            pagina.removeAllViews();
            if(estaNasInscricoes())videoDestaque=videos.get(0);
            else destaque(videos.get(0));
        }
        desenharFileira(id,grupo.getTitle()==null?"Para você":grupo.getTitle(),
                videos,principal);
        if(principal && podeExibirHubAoVivo()){
            final int geracao=geracaoPagina;
            if(!hubAoVivoPronto)
                postDelayed(()->{
                    if(geracao==geracaoPagina)adicionarHubAoVivo();
                },170);
            if(estaNaHome()&&!exploracaoPronta)
                postDelayed(()->{
                    if(geracao==geracaoPagina)adicionarExplorar();
                },400);
        }
    }
    private void desenharConfiguracoes(List<SettingsItem> opcoes){
        boolean entrar=entrarNoConteudoPendente || pagina.hasFocus();
        limparConteudo();
        if(opcoes==null||opcoes.isEmpty()){
            adicionarOpcaoConfiguracao("Atualizar aplicativo",
                    ()->AppDataSourceManager.verificarAtualizacaoHx(getContext()));
        }else{
            for(SettingsItem opcao:opcoes)
                if(opcao!=null)adicionarOpcaoConfiguracao(opcao.title,opcao.onClick);
        }
        if(focoPendente)post(this::tentarRestaurarFoco);
        if(entrar)post(()->{if(estaNasConfiguracoes())entrarNoConteudo();});
    }

    private void adicionarOpcaoConfiguracao(String titulo,Runnable acao){
        TextView item=texto(titulo,16,TEXTO,true);
        margem(item,18,8,18,8);
        foco(item);
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,dp(49));
        lp.bottomMargin=dp(7);
        pagina.addView(item,lp);
        if(acao!=null)item.setOnClickListener(v->acao.run());
        if(primeiroCartao==null)primeiroCartao=item;
    }

    @Override public void addSection(int indice,BrowseSection secao){
        executar(()->{
            if(secao==null)return;
            for(int i=secoes.size()-1;i>=0;i--)
                if(secoes.get(i).getId()==secao.getId())secoes.remove(i);
            int destino=indice<0||indice>secoes.size()?secoes.size():indice;
            secoes.add(destino,secao);
            renovarMenu();
        });
    }
    @Override public void removeSection(BrowseSection secao){
        executar(()->{
            if(secao!=null)
                for(int i=secoes.size()-1;i>=0;i--)
                    if(secoes.get(i).getId()==secao.getId())secoes.remove(i);
            renovarMenu();
        });
    }
    @Override
    protected void onAttachedToWindow(){
        super.onAttachedToWindow();
        PlaybackActivity.setHxPipObserver(hxPipObserver);
        post(this::atualizarReservaPiP);
    }

    @Override
    protected void onDetachedFromWindow(){
        PlaybackActivity.setHxPipObserver(null);
        limparEstadoSecao();
        geracaoPagina++;
        for(Disposable d:buscasAssuntos)if(d!=null&&!d.isDisposed())d.dispose();
        buscasAssuntos.clear();
        if(animacaoNav!=null)animacaoNav.cancel();
        if(destaquePendente!=null)removeCallbacks(destaquePendente);
        super.onDetachedFromWindow();
    }

    @Override public void removeAllSections(){
        executar(()->{secoes.clear();secaoAtual=-1;renovarMenu();});
    }
    @Override public void selectSection(int indice,boolean focoConteudo){
        executar(()->{
            if(secoes.isEmpty())return;
            abrirSecao(Math.min(Math.max(0,indice),secoes.size()-1));
            if(focoConteudo){
                entrarNoConteudoPendente=true;
                postDelayed(this::entrarNoConteudo,250);
            }else{
                postDelayed(()->{if(!hasFocus())focarCategoriaAtual();},150);
            }
        });
    }
    @Override public void updateSection(VideoGroup grupo){
        executar(()->{
            if(grupo==null||estaNasConfiguracoes())return;
            if(finalizarLoteAtualizacao!=null){
                loteAtualizacao.add(grupo);
                return;
            }
            // Quando o catálogo antigo ainda está na tela, espere um pequeno
            // lote de resultados antes de substituí-lo. Isso evita o flash de
            // uma fileira com um único card ao retornar da mini-janela.
            if(grupo.getAction()==VideoGroup.ACTION_REPLACE
                    && grupoPrincipalId!=null && !grupos.isEmpty()){
                loteAtualizacao.add(grupo);
                final int geracao=geracaoPagina;
                final int categoria=secaoAtual;
                finalizarLoteAtualizacao=()->{
                    finalizarLoteAtualizacao=null;
                    if(geracao!=geracaoPagina||categoria!=secaoAtual){
                        loteAtualizacao.clear();
                        return;
                    }
                    List<VideoGroup> entradas=new ArrayList<>(loteAtualizacao);
                    loteAtualizacao.clear();
                    for(VideoGroup entrada:entradas)desenharGrupo(entrada);
                    carregando.setVisibility(View.GONE);
                    if(focoPendente)post(this::tentarRestaurarFoco);
                };
                postDelayed(finalizarLoteAtualizacao,300);
                return;
            }
            desenharGrupo(grupo);
            carregando.setVisibility(View.GONE);
        });
    }
    @Override public void updateSection(SettingsGroup grupo){
        executar(()->{
            if(!estaNasConfiguracoes())return;
            desenharConfiguracoes(grupo==null?null:grupo.getItems());
            carregando.setVisibility(View.GONE);
        });
    }
    @Override public void clearSection(BrowseSection secao){
        executar(()->{
            limparConteudo();
            if(secaoAtual>=0)mostrarCarregamentoSecao();
        });
    }
    @Override public void selectSectionItem(int indice){focusOnContent();}
    @Override public void selectSectionItem(Video video){focusOnContent();}
    @Override public void focusOnContent(){
        executar(this::entrarNoConteudo);
    }
    @Override public void showError(ErrorFragmentData erro){
        executar(()->{
            limparConteudo();
            TextView msg=texto(erro==null?"Não foi possível carregar este conteúdo":
                    erro.getMessage(),17,TEXTO,true);
            margem(msg,5,25,0,20);pagina.addView(msg);
            if(erro!=null && erro.getActionText()!=null){
                TextView tentar=texto(erro.getActionText(),15,GELO,true);
                margem(tentar,12,10,12,10);foco(tentar);
                tentar.setOnClickListener(v->erro.onAction());
                pagina.addView(tentar);
            }
        });
    }
    @Override public void showProgressBar(boolean mostrar){
        executar(()->carregando.setVisibility(mostrar?View.VISIBLE:View.GONE));
    }
    @Override public boolean isProgressBarShowing(){
        return carregando.getVisibility()==View.VISIBLE;
    }
    @Override public boolean isEmpty(){return estaNasConfiguracoes()?primeiroCartao==null:grupos.isEmpty();}
    @Override public void updateBadge(){} // Marca HX já é nativa no cabeçalho.

    /** Conserva a seleção antes do player tomar o foco da janela. */
    public void guardarFocoAntesDeSair(){
        View ativo=findFocus();
        if(ativo!=null && ativo!=this){
            ultimoControleFocado=ativo;
            estavaNoMenu=menu.hasFocus();
            ultimaSecaoFocada=secaoAtual;
        }
        voltarDaReproducao=true;
        focoPendente=true;
    }

    /** Recupera o foco quando a janela da TCL volta após a reprodução. */
    public void restaurarFocoAposReproducao(){
        atualizarReservaPiP();
        postDelayed(this::atualizarReservaPiP,450);
        if(!voltarDaReproducao)return;
        focoPendente=true;
        postDelayed(this::tentarRestaurarFoco,100);
        postDelayed(this::tentarRestaurarFoco,340);
    }

    private void tentarRestaurarFoco(){
        if(!focoPendente || !isAttachedToWindow() || !hasWindowFocus())return;
        View alvo=null;
        if(ultimoControleFocado!=null && ultimoControleFocado.isAttachedToWindow()
                && ultimoControleFocado.isShown() && ultimoControleFocado.isFocusable()){
            alvo=ultimoControleFocado;
        }else if(ultimaSecaoFocada==secaoAtual && ultimoVideoFocado!=null){
            alvo=cartoesPorId.get(ultimoVideoFocado);
        }
        if(alvo==null && estavaNoMenu)alvo=itemCategoriaAtual();
        if(alvo==null)alvo=primeiroCartao;
        if(alvo!=null && alvo.isShown() && alvo.requestFocus()){
            focoPendente=false;
            voltarDaReproducao=false;
        }
    }

    /** Voltar, quando dentro dos vídeos, retorna à navegação lateral. */
    public boolean voltarAoMenu(){
        View categoria=itemCategoriaAtual();
        if(categoria!=null&&!menu.hasFocus()){
            if(!focarCategoriaAtual())return false;
            ultimoControleFocado=categoria;
            estavaNoMenu=true;
            ultimaSecaoFocada=secaoAtual;
            return true;
        }
        return false;
    }
}
