package com.javi.controlhorario

import java.util.Properties
import javax.mail.Authenticator
import javax.mail.Message
import javax.mail.PasswordAuthentication
import javax.mail.Session
import javax.mail.Transport
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeMessage

object EmailSender {
    /** Envía por SMTP de Gmail (STARTTLS, puerto 587). Debe llamarse fuera del hilo principal. */
    fun send(s: Settings, subject: String, body: String) {
        require(s.smtpPass.isNotBlank()) { "Falta la contraseña de aplicación de Gmail en Ajustes" }
        val props = Properties().apply {
            put("mail.smtp.auth", "true")
            put("mail.smtp.starttls.enable", "true")
            put("mail.smtp.host", "smtp.gmail.com")
            put("mail.smtp.port", "587")
            put("mail.smtp.connectiontimeout", "15000")
            put("mail.smtp.timeout", "15000")
        }
        val session = Session.getInstance(props, object : Authenticator() {
            override fun getPasswordAuthentication() = PasswordAuthentication(s.smtpUser, s.smtpPass.replace(" ", ""))
        })
        val msg = MimeMessage(session).apply {
            setFrom(InternetAddress(s.smtpUser))
            setRecipients(Message.RecipientType.TO, InternetAddress.parse(s.to))
            setSubject(subject, "UTF-8")
            setText(body, "UTF-8")
        }
        Transport.send(msg)
    }
}
