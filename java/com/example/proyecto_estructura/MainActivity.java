package com.example.proyecto_estructura;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import com.ibm.watson.developer_cloud.natural_language_understanding.v1.NaturalLanguageUnderstanding;
import com.ibm.watson.developer_cloud.natural_language_understanding.v1.model.AnalysisResults;
import com.ibm.watson.developer_cloud.natural_language_understanding.v1.model.AnalyzeOptions;
import com.ibm.watson.developer_cloud.natural_language_understanding.v1.model.EntitiesOptions;
import com.ibm.watson.developer_cloud.natural_language_understanding.v1.model.Features;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONObject;

public class MainActivity extends Activity {

    private EditText eTDiagnostico;
	private ImageButton btnSpeak;
    private EditText eTCodigo;
    private EditText eTDescripcion;
    private EditText eTNombre;
    private EditText eTEdad;
    private Button btnCodigo;
    private Button btnGuardar;
	private final int REQ_CODE_SPEECH_INPUT = 100;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

        eTDiagnostico = (EditText) findViewById(R.id.eTDiagnostico);
        eTCodigo = (EditText) findViewById(R.id.eTCodigo);
        eTDescripcion = (EditText) findViewById(R.id.eTDescripcion);
        eTNombre = (EditText) findViewById(R.id.eTNombre);
        eTEdad = (EditText) findViewById(R.id.eTEdad);
        btnCodigo = (Button) findViewById(R.id.btnCodigo);
		btnSpeak = (ImageButton) findViewById(R.id.btnSpeak);
        btnGuardar = (Button) findViewById(R.id.btnGuardar);

		btnSpeak.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				promptSpeechInput();
			}
		});

        btnCodigo.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                eTCodigo.setText("");
                eTDescripcion.setText("");

                if(!eTDiagnostico.getText().toString().isEmpty()) {
                    if(eTNombre.getText().toString().isEmpty()){
                        eTNombre.setHint("Nombre desconocido");
                    }
                    if(eTEdad.getText().toString().isEmpty()){
                        eTEdad.setHint("Edad desconocida");
                    }

                    String str = eTDiagnostico.getText().toString();
                    List<String> elephantList = Arrays.asList(str.split(",[ ]*"));

                    for(int i=0; i<elephantList.size();i++){
                        new AccesarWebService().execute(elephantList.get(i));
                    }

                }else{
                    new AlertDialog.Builder(MainActivity.this)
                            .setTitle("Atención")
                            .setMessage("Campo de diagnóstico vacío.")
                            .setCancelable(true)
                            .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                }
                            })
                            .setIcon(android.R.drawable.ic_menu_info_details)
                            .show();
                }
            }
        });

        btnGuardar.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {

                eTDiagnostico.setText("");
                eTCodigo.setText("");
                eTNombre.setText("");
                eTEdad.setText("");
                eTDescripcion.setText("");

                eTDiagnostico.setHint("");
                eTCodigo.setHint("");
                eTNombre.setHint("");
                eTEdad.setHint("");
                eTDescripcion.setHint("");

                new AlertDialog.Builder(MainActivity.this)
                        .setTitle("Atención")
                        .setMessage("Reporte de consulta guardado exitosamente.")
                        .setCancelable(true)
                        .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                            }
                        })
                        .setIcon(android.R.drawable.ic_menu_info_details)
                        .show();
            }
        });

	}

	private void promptSpeechInput() {
		Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
		intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
				RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
		intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
		intent.putExtra(RecognizerIntent.EXTRA_PROMPT,
				getString(R.string.speech_prompt));
		try {
			startActivityForResult(intent, REQ_CODE_SPEECH_INPUT);
		} catch (ActivityNotFoundException a) {
			Toast.makeText(getApplicationContext(),
					getString(R.string.speech_not_supported),
					Toast.LENGTH_SHORT).show();
		}
	}

	@Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case REQ_CODE_SPEECH_INPUT: {
                if (resultCode == RESULT_OK && null != data) {

                    ArrayList<String> result = data
                            .getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                    String s = result.get(0);
                    s = s.replaceAll("Cáncer","cáncer");
                    s = s.replaceAll("Cancer","cáncer");
                    s = s.replaceAll("Sida","sida");
                    s = s.replaceAll("Sufre","sufre");
                    new RetrieveFeedTask().execute(s);
                }
                break;
            }
        }
    }

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}


	class RetrieveFeedTask extends AsyncTask<String, Void, AnalysisResults> {

		private Exception exception;
		ProgressDialog pd;

		@Override
		protected void onPreExecute() {
			pd = new ProgressDialog(MainActivity.this);
			pd.setMessage("Interpretando...");
			pd.show();
		}

		protected AnalysisResults doInBackground(String... text) {
			try {
				NaturalLanguageUnderstanding service;
				String username = "2c61d1c1-8928-4790-869b-ede27e4bbeae";
				String password = "eMF7QCZJ7fx8";

				service = new NaturalLanguageUnderstanding(NaturalLanguageUnderstanding.VERSION_DATE_2017_02_27);
				service.setUsernameAndPassword(username, password);
				service.setEndPoint("https://gateway.watsonplatform.net/natural-language-understanding/api");

				EntitiesOptions entities = new EntitiesOptions.Builder()
						.limit(20)
						.sentiment(false)
						.model("20:d0817f58-fed7-4988-aaf5-fae6c4ee50e3")
						.build();

				Features features = new Features.Builder()
						.entities(entities)
						.build();

				AnalyzeOptions parameters = new AnalyzeOptions.Builder()
						.text(text[0])
						.features(features)
						.returnAnalyzedText(true)
						.build();

				AnalysisResults results = service.analyze(parameters).execute();


				return results;

			} catch (Exception e) {
				this.exception = e;

				return null;
			}
		}

        protected void onPostExecute(AnalysisResults results) {
            pd.dismiss();
            System.out.println(results);
            if (results != null) {

                for (int i = 0; i < results.getEntities().size(); i++) {
                    if (results.getEntities().get(i).getType().equals("Nombre")) {
                        if(!eTNombre.getText().toString().isEmpty()){
                            eTNombre.setText(eTNombre.getText().toString() + " ");
                        }
                        eTNombre.setText(eTNombre.getText().toString() + results.getEntities().get(i).getText());
                    }
                    if (results.getEntities().get(i).getType().equals("Edad")) {
                        eTEdad.setText(results.getEntities().get(i).getText());
                    }
                    if (results.getEntities().get(i).getType().equals("Diagnostico")) {
                        if(!eTDiagnostico.getText().toString().isEmpty()){
                            eTDiagnostico.setText(eTDiagnostico.getText().toString() + ", ");
                        }
                        eTDiagnostico.setText(eTDiagnostico.getText().toString() + results.getEntities().get(i).getText());

                        new AccesarWebService().execute(results.getEntities().get(i).getText());
                    }
                }

                if(eTNombre.getText().toString().isEmpty()){
                    eTNombre.setHint("Nombre desconocido");
                }
                if(eTEdad.getText().toString().isEmpty()){
                    eTEdad.setHint("Edad desconocida");
                }
                if(eTDiagnostico.getText().toString().isEmpty()){
                    eTDiagnostico.setHint("Ingrese manualmente");
                    new AlertDialog.Builder(MainActivity.this)
                            .setTitle("Advertencia")
                            .setMessage("No se identificó ningun diagnóstico, intentelo nuevamente o ingreselo manualmente.")
                            .setCancelable(true)
                            .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                }
                            })
                            .setIcon(android.R.drawable.ic_menu_info_details)
                            .show();
                }

            }else{
                new AlertDialog.Builder(MainActivity.this)
                        .setTitle("Error de conexión")
                        .setMessage("Verifique la conexión a internet.")
                        .setCancelable(true)
                        .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                            }
                        })
                        .setIcon(android.R.drawable.ic_delete)
                        .show();
            }
        }
    }


    class AccesarWebService extends AsyncTask<String, Void, String> {

        private Exception exception;
        private ProgressDialog pd;
        private String rawDiagnostico;

        @Override
        protected void onPreExecute() {
            pd = new ProgressDialog(MainActivity.this);
            pd.setMessage("Buscando Codigos...");
            pd.show();
        }

        protected String doInBackground(String... text) {
            String result = null;

            rawDiagnostico = text[0];

            String diagnosticoURL = StringUtils.stripAccents(rawDiagnostico).replaceAll(" ", "%20");

            try {
                URL myurl = new URL("http://uvgproyectos.esaludgt.org/web/Api/Codigos?diagnostico=" + diagnosticoURL);
                HttpURLConnection urlConnection = (HttpURLConnection) myurl
                        .openConnection();
                urlConnection.setRequestMethod("GET");
                urlConnection.setDoInput(true);
                urlConnection.connect();
                InputStream is = urlConnection.getInputStream();
                if (is != null) {
                    StringBuilder sb = new StringBuilder();
                    String line;
                    try {
                        BufferedReader reader = new BufferedReader(
                                new InputStreamReader(is));
                        while ((line = reader.readLine()) != null) {
                            sb.append(line);
                        }
                        reader.close();
                    } finally {
                        is.close();
                    }
                    result = sb.toString();
                }
            } catch (Exception e) {
                e.printStackTrace();
                result = null;
            }
            return result;
        }

        protected void onPostExecute(String results) {
            pd.dismiss();

            try {
                JSONObject jObject = new JSONObject(results);
                String codigo = jObject.getString("Key");
                String descripcion = jObject.getString("Value");

                if(!eTCodigo.getText().toString().isEmpty()){
                    eTCodigo.setText(eTCodigo.getText().toString() + ", ");
                }
                if(!eTDescripcion.getText().toString().isEmpty()){
                    eTDescripcion.setText(eTDescripcion.getText().toString() + "; ");
                }

                if(!codigo.equals("Not Found")) {
                    eTCodigo.setText(eTCodigo.getText().toString() + codigo);

                    eTDescripcion.setText(eTDescripcion.getText().toString() + descripcion);
                }else{
                    eTCodigo.setText(eTCodigo.getText().toString() + "(" + rawDiagnostico + ")");
                    eTDescripcion.setText(eTDescripcion.getText().toString() + "(" + rawDiagnostico + ")");
                    eTCodigo.setHint("Ingrese manualmente");
                    eTDescripcion.setHint("Ingrese manualmente");
                    new AlertDialog.Builder(MainActivity.this)
                            .setTitle("Advertencia")
                            .setMessage("No se encontró el código del diagnóstico \"" + rawDiagnostico + "\" en la base de datos. Si lo conoce ingreselo manualmente.")
                            .setCancelable(true)
                            .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                }
                            })
                            .setIcon(android.R.drawable.ic_menu_info_details)
                            .show();
                }

            } catch (Exception e) {
                new AlertDialog.Builder(MainActivity.this)
                        .setTitle("Error de conexión")
                        .setMessage("Verifique la conexión a internet.")
                        .setCancelable(true)
                        .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                            }
                        })
                        .setIcon(android.R.drawable.ic_delete)
                        .show();
            }
        }
    }
}