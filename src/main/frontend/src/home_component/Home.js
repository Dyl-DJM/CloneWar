import './Home.css';
import React from 'react';

/**
 * Return the HTML element that represents the title of the page.
 *
 * @param goBackHomeOnClick true if a click on returned HTML element should redirect to home, false otherwise
 * @param functionToDoOnClick a function to do on click on the returned HTML element if goBackHomeOnClick is true
 *
 * @return the HTML element that represents the title of the page
 */
function title(goBackHomeOnClick, functionToDoOnClick) {
    if(goBackHomeOnClick) {
        return(
            <div onClick={functionToDoOnClick}>
                <h1 class="title is-1 elem has-text-centered">CLONEWAR</h1>
                <h2 class="subtitle is-2 elem has-text-centered">Envers et contre tout plagiat</h2>
            </div>
        );
    }
    return(
        <div>
            <h1 class="title is-1 elem has-text-centered">CLONEWAR</h1>
            <h2 class="subtitle is-2 elem has-text-centered">Envers et contre tout plagiat</h2>
        </div>
    );
}

/**
 * Return the HTML element that represents the footer of the page.
 *
 * @return the HTML element that represents the footer of the page
 */
function footer() {
    return (
        <div>
            <footer>
                <div class="content has-text-centered footer">
                    <p>
                        <strong>CLONEWAR - Envers et contre tout plagiat</strong> © - 2022 par Dylan DE JESUS & Vincent RICHARD - <a href="https://gitlab.com/m1-info-2022/de_jesus-richard">Projet open source sur GitLab</a>
                    </p>
                </div>
            </footer>
        </div>
    );
}

/**
 * Return the HTML element that represents a notification containing the result of an analyze.
 *
 * @param the result of an analyze
 *
 * @return the HTML element that represents a notification containing the result of an analyze
 */
function displayResultAnalysis(resultAnalysis) {
    if(resultAnalysis === "") {
        return (<span></span>);
    }
    return (
        <div class="notification is-link is-light">
            {resultAnalysis}
        </div>
    );
}

/**
 * Return the HTML element that represents a notification containing the result of a comparison betwin two analyzed artifacts.
 *
 * @param the result of a comparison betwin two analyzed artifacts
 *
 * @return the HTML element that represents a notification containing the result of a comparison betwin two analyzed artifacts
 */
function displayResultValue(resultValue) {
    if(resultValue === "") {
        return (<span></span>);
    }
    return (
        <div class="notification is-link is-light">
            {resultValue}
        </div>
    );
}

/**
 * Home class.
 */
class Home extends React.Component {
    
    /**
     * Class constructor.
     * 
     * @param {*} props 
     */
    constructor(props){
        super(props);
        this.state = {
            needArtefactsListRefresh: true,
            name: props.name,
            resultAnalysis: '',
            resultValue: '',
            artifactsListAsString: '',
            resultArtifactsList: '',
            resultArtifactsListForReferenceSelection: '',
            resultArtifactsListForCloneSelection: '',
            jar: '',
            referenceArtifact:'',
            cloneArtifact:'',
            displays_artpage: false}; // Components default state
        this.submitAnalysis = this.submitAnalysis.bind(this);
        this.submitComparison = this.submitComparison.bind(this);
        this.changePage = this.changePage.bind(this);
        this.setJar = this.setJar.bind(this);
        this.setReferenceArtifact = this.setReferenceArtifact.bind(this);
        this.setCloneArtifact = this.setCloneArtifact.bind(this);
    }

    setJar(event) {
        this.setState({jar: event.target.value});
        event.preventDefault();
    }

    setReferenceArtifact(event) {
        this.setState({referenceArtifact: event.target.value});
        event.preventDefault();
    }

    setCloneArtifact(event) {
        this.setState({cloneArtifact: event.target.value});
        event.preventDefault();
    }

    async getAnalyseResponse(jarPathToAnalyseAsString) {
      try {
        var response = await fetch("http://127.0.0.1:8080/analyze?jarPathToAnalyse=" + jarPathToAnalyseAsString.replace(/%20/g, " ").trim());
        if (!response.ok) {
          this.setState({resultAnalysis: `Veuillez vérifier que le serveur est lancé et que le chemin que vous avez entré est correct. Une erreur est survenue => HTTP error: ${response.status}`});
          throw new Error(`Veuillez vérifier que le serveur est lancé et que le chemin que vous avez entré est correct. Une erreur est survenue => HTTP error: ${response.status}`);
        }
        var data = await response.text();
        console.log(data);
        this.setState({resultAnalysis: data});
        this.setState({jar: ''});
        this.setState({needArtefactsListRefresh : true});
      }
      catch (error) {
        console.error(`Veuillez vérifier que le serveur est lancé et que le chemin que vous avez entré est correct. Une erreur est survenue => Could not get data: ${error}`);
        this.setState({resultAnalysis: `Veuillez vérifier que le serveur est lancé et que le chemin que vous avez entré est correct. Une erreur est survenue => Could not get data: ${error}`});
      }
    }

    /**
     * Called when the analysis form is submitted. Displays an alert which can be either a warning
     * message to explain that the form isn't fullfilled correctly or displays a message saying that
     * the artefact has successfully been registered.
     *
     * @param {*} event which calls the event.
     */
    submitAnalysis(event){
        if(this.state.jar === ''){
            alert("Veuillez déposer un artéfact à analyser, s'il vous plaît.");
        }
        else{
            this.setState({resultAnalysis: <progress class="progress is-warning" max="100"></progress>});
            this.getAnalyseResponse(this.state.jar);
        }
        event.preventDefault();
    }

    async getAlreadyAnalyzedJars() {
      try {
        var response = await fetch("http://127.0.0.1:8080/analyzedJars");
        if (!response.ok) {
          this.setState({artifactsListAsString: `Veuillez vérifier que le serveur est lancé et que le chemin que vous avez entré est correct. Une erreur est survenue => HTTP error: ${response.status}`});
          throw new Error(`Veuillez vérifier que le serveur est lancé et que le chemin que vous avez entré est correct. Une erreur est survenue => HTTP error: ${response.status}`);
        }
        var data = await response.text();
        console.log(data);
        this.setState({artifactsListAsString: data});
      }
      catch (error) {
        console.error(`Veuillez vérifier que le serveur est lancé et que le chemin que vous avez entré est correct. Une erreur est survenue => Could not get data: ${error}`);
        this.setState({artifactsListAsString: `Veuillez vérifier que le serveur est lancé et que le chemin que vous avez entré est correct. Une erreur est survenue => Could not get data: ${error}`});
      }
    }

    submitArtifactsList(){
        if (this.state.artifactsListAsString !== "") {
            var artifactsList = this.state.artifactsListAsString.split("\n").map((artifact) => <li class="box" onClick={this.changePage}>{artifact}</li>);
            this.setState({resultArtifactsList: artifactsList});
        }
    }

    submitArtifactsListForReferenceSelection(){
        if (this.state.artifactsListAsString !== "") {
            var artifactsListForReferenceSelection = this.state.artifactsListAsString.split("\n").map((artifact) => <option name='referenceArtifact' value={artifact}>{artifact}</option>);
            this.setState({resultArtifactsListForReferenceSelection: artifactsListForReferenceSelection});
        }
    }

    submitArtifactsListForCloneSelection(){
        if (this.state.artifactsListAsString !== "") {
            var artifactsListForCloneSelection = this.state.artifactsListAsString.split("\n").map((artifact) => <option name='cloneArtifact' value={artifact}>{artifact}</option>);
            this.setState({resultArtifactsListForCloneSelection: artifactsListForCloneSelection});
        }
    }

    async refreshArtifactsList(){
        await this.getAlreadyAnalyzedJars();
        this.submitArtifactsList();
        this.submitArtifactsListForReferenceSelection();
        this.submitArtifactsListForCloneSelection();
        this.setState({needArtefactsListRefresh : false});
        if(this.state.needArtefactsListRefresh){
            this.refreshArtifactsList();
        }
    }

    async getResultComparison(referenceArtifact, cloneArtifact) {
      try {
        var response = await fetch("http://127.0.0.1:8080/percentageCloning?referenceArtifact=" + referenceArtifact.replace(/%20/g, " ").trim() + "&cloneArtifact=" + cloneArtifact.replace(/%20/g, " ").trim());
        if (!response.ok) {
          this.setState({resultValue: `Veuillez vérifier que le serveur est lancé et que le chemin que vous avez entré est correct. Une erreur est survenue => HTTP error: ${response.status}`});
          throw new Error(`Veuillez vérifier que le serveur est lancé et que le chemin que vous avez entré est correct. Une erreur est survenue => HTTP error: ${response.status}`);
        }
        var data = await response.text();
        console.log(data);
        this.setState({resultValue: data});
        this.setState({referenceArtifact: ''});
        this.setState({cloneArtifact: ''});
        document.getElementById("referenceArtifactSelect").selectedIndex = 0;
        document.getElementById("cloneArtifactSelect").selectedIndex = 0;
      }
      catch (error) {
        console.error(`Veuillez vérifier que le serveur est lancé et que le chemin que vous avez entré est correct. Une erreur est survenue => Could not get data: ${error}`);
        this.setState({resultValue: `Veuillez vérifier que le serveur est lancé et que le chemin que vous avez entré est correct. Une erreur est survenue => Could not get data: ${error}`});
      }
    }

    submitComparison(event){
        if(this.state.referenceArtifact === ''){
            alert("Veuillez déposer un artéfact de référence, s'il vous plaît.");
        }
        else if(this.state.cloneArtifact === ''){
            alert("Veuillez déposer un artéfact à vérifier, s'il vous plaît.");
        } else {
            this.setState({resultValue: <progress class="progress is-warning" max="100"></progress>});
            this.getResultComparison(this.state.referenceArtifact, this.state.cloneArtifact);
        }
        event.preventDefault();
    }

    /**
     * Switch the state of the artefact page display.
     *
     * @param {*} event
     */
    changePage(){
        this.setState({displays_artpage : !this.state.displays_artpage});
    }

    /**
     * Returns the DOM element of the class.
     * 
     * @returns 
     */
    render(){
        if(this.state.needArtefactsListRefresh){
            this.refreshArtifactsList();
        }
        if(this.state.displays_artpage === false){
            return (
                <div>
                    {title(false, '')}
                    <section class="hero is-warning">
                      <div class="hero-body">
                        <p class="has-text-centered">
                            L&#39;application CLONEWAR fonctionnant avec un serveur Helidon Nima 4.0, actuellement en alpha, un bug interne aux serveurs Helidon Nima fait qu&#39;il se peut que lors d&#39;une actualisation de cette page web, celle-ci ne s&#39;affiche plus.
                            <br/><br/>
                            Pas de panique. Il vous suffira de recharger de nouveau la page et votre guerre contre le plagiat pourra reprendre son cours.
                        </p>
                      </div>
                    </section>
                    <section class="hero is-info">
                      <div class="hero-body">
                        <p class="title">
                            Légalement, qu&#39;est ce que le plagiat ?
                        </p>
                        <p class="has-text-centered">
                            Le plagiat est un délit, il se définit comme &#34; toute reproduction, représentation ou diffusion, par quelque moyen que ce soit, d&#39;une œuvre de l&#39;esprit en violation des droits de l&#39;auteur, tels qu&#39;ils sont définis et réglementés par la loi".
                            <br/><br/>
                            <li>L&#39;article L335-3 du Code de la Propriété Intellectuelle assimile le plagiat au délit de contrefaçon : &#34; Le plagiat se caractérise par un copier-coller dans une copie, l&#39;appropriation de certaines phrases idées, citations, sans mention de l&#39;auteur, etc."</li>
                            <li>L&#39;article L112-1 du Code de la Propriété Intellectuelle dispose que toute représentation ou reproduction intégrale ou partielle faite sans le consentement de l&#39;auteur ou de ses ayants droit ou ayants cause est illicite.</li>
                            <li>L&#39;article L112-4 du Code la Propriété Intellectuelle l&#39;étant aux traductions, adaptations, transformations, arrangements ou encore reproduction par un art ou procédé quelconque.</li>
                        </p>
                        <p class="subtitle">
                            <br/><br/>
                            <strong>Source :</strong> <a href="https://www.enseignementsup-recherche.gouv.fr/fr/periode-d-examen-attention-plagiat-48655"><u>Ministère de l&#39;ensignement supérieur et de la recherche</u></a>
                        </p>
                      </div>
                    </section>

                    <section class="hero is-danger">
                      <div class="hero-body">
                        <p class="title">
                            Concrètement, quelles sont les peines encourues ?
                        </p>
                        <p class="has-text-centered">
                            Il existe plusieurs logiciels &#34;anti-plagiat&#34; qui détectent toutes les parties et citations, utilisées dans une copie, issues d&#39;internet et qui en retrouvent la source.
                            Les sanctions encourues sont très élevées. L&#39;établissement d&#39;enseignement supérieur peut convoquer la Section Disciplinaire. L&#39;étudiant inquiété risque, selon le degré de gravité, un zéro à l&#39;épreuve jusqu&#39;à l&#39;exclusion définitive de tout établissement d&#39;enseignement supérieur pendant une durée maximale de cinq ans.
                            Autrement, un recours peut être formé devant les instances civiles et pénales. Les risques encourus sont :
                            <br/><br/>
                            <li>des dommages et intérêts</li>
                            <li>150 000 euros d&#39;amende</li>
                            <li>jusqu&#39;à deux ans de prison</li>
                        </p>
                        <p class="subtitle">
                            <br/><br/>
                            <strong>Source :</strong> <a href="https://www.enseignementsup-recherche.gouv.fr/fr/periode-d-examen-attention-plagiat-48655"><u>Ministère de l&#39;ensignement supérieur et de la recherche</u></a>
                        </p>
                      </div>
                    </section>

                    <section class="hero">
                      <div class="hero-body">
                        <p class="title">
                            Analysez tous les artéfacts que vous désirez
                        </p>
                        <p class="has-text-centered">
                            Un artéfact est constitué de deux archives différentes, l&#39;archive &#34;main&#34; contient le bytecode (les .class) de l&#39;artéfact et l&#39;archive &#34;source&#34; contient le code source associé (les .java).
                            Pour analyser un artéfact, veuillez cliquer sur le bouton &#34;Choisir un fichier&#34; puis cliquer sur le bouton &#34;Analyser&#34;.
                            <br/><br/>
                            Toute analyse sera sauvegardée et rechargée après fermeture et réouverture du programme.
                            L&#39;analyse d&#39;un artéfact permet d&#39;extraire toutes les instructions importantes de celui-ci.
                            Cela permettra, dans la section suivante, de pouvoir comparer deux artéfacts et de détecter s&#39;il y a plagiat ou non.
                            <br/><br/>
                            Entrez ci-dessous le chemin absolu de l&#39;artéfact que vous désirez analyser.
                            <br/>
                            Par exemple, sur Linux ou Mac : /home/nom_utilisateur/mes_artefacts/arefact.jar
                            <br/>
                            Par exemple, sur Windows : C:\Users\nom_utilisateur\Desktop\mes_artefacts\artefact.jar
                            <br/><br/>
                            Un petit conseil : Vous pouvez récupérer ce chemin absolu depuis votre explorateur de fichiers et copier puis coller ce chemin directement ici.
                            <br/><br/>
                            L&#39;analyse d&#39;un artéfact peut prendre plus ou moins de temps en fonction de la taille de celui-ci. Une alerte apparaîtra sur votre écran lorsque l&#39;analyse sera terminée.
                        </p>
                        <form onSubmit={this.submitAnalysis} class="global">
                            <div class="field">
                                <label class="label">Artéfact à analyser</label>
                                <div class="control">
                                    <input class="input is-medium" type="text" placeHolder="/home/nom_utilisateur/mes_artefacts/arefact.jar" name='jar' value={this.state.jar} onChange={this.setJar}/>
                                </div>
                            </div>
                            <button type="submit" class="button is-link is-rounded">Analyser l&#39;artéfact</button>
                        </form>
                        {displayResultAnalysis(this.state.resultAnalysis)}
                      </div>
                    </section>

                    <section class="hero is-primary">
                      <div class="hero-body">
                        <p class="title">
                            Liste des artéfacts déjà analysés :
                        </p>
                        <p class="has-text-centered">
                            Si un artéfact à été modifié, vous pouvez réaliser une nouvelle analyse sur celui-ci.
                            Cela aura pour effet de remplacer les informations le concernant.
                            <br/><br/>
                            Cliquez sur l&#39;un des artéfacts ci-dessous pour consulter les informations qui lui sont relatif.
                        </p>
                        <div class="artifact-list">
                            {this.state.resultArtifactsList}
                        </div>
                      </div>
                    </section>

                    <section class="hero">
                        <div class="hero-body">
                            <p class="title">
                              Détécter le plagiat en quelques clics est désormais possible !
                            </p>
                            <p class="has-text-centered">
                              Détécter si deux artéfacts ont des similitudes, voir des codes commun peut petre très difficile manuellement.
                              De plus, il est également très facile de rendre cela indétectable à oeil nu en modifiant
                              le nom des classes, champs, méthodes et variables, en inverssant des instructions ou en remplacant des instructions par d&#39;autres qui ont le même comportement.
                              <br/><br/>
                              Grâce à CLONEWAR, une fois que vous avez analysé deux artéfacts, il vous suffit de les renseigner ci-dessous et de cliquer sur le bouton &#34;Calculer le taux de plagiat&#34;.
                              Le premier artéfact correpondant à l&#39;artéfact de référence, l&#39;artéfact original tandis que le second artéfact correpond à celui sur lequel vous avez des doutes.
                              <br/><br/>
                              Allez, n&#39;hésitez pas ! CLONEWAR calculera pour vous le pourcentage de plagiat du second artéfact et le pourcentage de données récupérées depuis l&#39;artéfact de référence.
                              <br/><br/>
                              Le calcul du taux de plagiat entre de deux artéfacts peut prendre plus ou moins de temps en fonction de la taille de ceux-ci. Les taux calculés apparaîtront sur votre écran lorsque celui-ci sera terminé.
                            </p>
                            <form onSubmit={this.submitComparison}>
                                <div class="field comparisonFormField">
                                    <label class="label">Artéfact de référence</label>
                                    <div class="select is-medium">
                                        <select id="referenceArtifactSelect" onChange={this.setReferenceArtifact}>
                                            <option value=''>Séléctionner un artéfact</option>
                                            {this.state.resultArtifactsListForReferenceSelection}
                                        </select>
                                    </div>
                                </div>
                                <div class="field comparisonFormField">
                                    <label class="label">Artéfact à vérifier</label>
                                    <div class="select is-medium">
                                        <select id="cloneArtifactSelect" onChange={this.setCloneArtifact}>
                                            <option value=''>Séléctionner un artéfact</option>
                                            {this.state.resultArtifactsListForCloneSelection}
                                        </select>
                                    </div>
                                </div>
                                <button type="submit" class="button is-link is-rounded">Calculer le taux de plagiat</button>
                            </form>
                            {displayResultValue(this.state.resultValue)}
                        </div>
                    </section>
                    {footer()}
                </div>
            )
        }else{
            return(
                <div>
                    <div>
                        {title(true, this.changePage)}
                        <section class="hero is-primary">
                          <div class="hero-body">
                            <p class="title">
                              Détails de l&#39;artéfact séléctionné : {this.props.artefact}
                            </p>
                            <li>Nom : </li>
                            <li>Auteur : </li>
                            <li>Chemin : </li>
                            <li>Taille : </li>
                            <li>Date de dernière modification : </li>
                          </div>
                        </section>

                        <button class="button is-large is-responsive is-rounded" onClick={this.changePage}>Retour</button>
                    </div>
                    {footer()}
                </div>
            )
        }
    }
}

export default Home;
